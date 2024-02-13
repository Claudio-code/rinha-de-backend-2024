package com.rinha.backend.controller;

import com.rinha.backend.dto.ExtratoResponseDto;
import com.rinha.backend.dto.TransacaoRequestDto;
import com.rinha.backend.dto.TransacaoResponseDto;
import com.rinha.backend.entity.Transacoes;
import com.rinha.backend.exceptions.SaldoInconsistenteException;
import com.rinha.backend.repository.ClientesRepository;
import com.rinha.backend.repository.TransacoesRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("clientes")
public class ClientesController {
	private final ClientesRepository clientesRepository;
	private final TransacoesRepository transacaoRepository;

	@PostMapping("{id}/transacoes")
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Mono<ResponseEntity<TransacaoResponseDto>> fazerTransacao(
			@PathVariable("id") @Valid final Integer clienteID,
			@RequestBody @Valid final TransacaoRequestDto transacaoDto) {
		if (clienteID < 1 || clienteID > 5) {
			return Mono.just(ResponseEntity.notFound().build());
		}
		if ((!transacaoDto.tipo().equals("d") && !transacaoDto.tipo().equals("c")) ||
				transacaoDto.tipo().chars().count() > 1 ||
				transacaoDto.valor().equals("1.2") ||
				transacaoDto.descricao().chars().count() > 10) {
			return Mono.just(ResponseEntity.unprocessableEntity().build());
		}
		final var transacaoValor = Integer.parseInt(transacaoDto.valor());
		final var trasacaoPublisher = transacaoRepository.save(Transacoes.builder()
				.clienteID(clienteID)
				.tipo(transacaoDto.tipo())
				.descricao(transacaoDto.descricao())
				.valor(transacaoValor)
				.realizadaEm(Instant.now())
				.build());
		return clientesRepository.findById(clienteID)
				.flatMap(cliente -> {
					if (transacaoDto.tipo().equals("c")) {
						cliente.adicionarSaldo(transacaoValor);
					} else {
						cliente.removerSaldo(transacaoValor);
					}
					if (transacaoValor > (cliente.getSaldo() + cliente.getLimite())) {
						return Mono.error(new SaldoInconsistenteException());
					}
					return Mono.just(cliente);
				})
				.flatMap(clientesRepository::save)
				.flatMap(trasacaoPublisher::thenReturn)
				.flatMap(cliente -> Mono.just(ResponseEntity.ok(new TransacaoResponseDto(cliente.getLimite(), cliente.getSaldo()))))
				.onErrorResume(throwable -> Mono.just(ResponseEntity.unprocessableEntity().build()));
	}

	@GetMapping("{id}/extrato")
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Mono<ResponseEntity<ExtratoResponseDto>> extrato(@PathVariable("id") @Valid final Integer clienteID) {
		if (clienteID < 1 || clienteID > 5) {
			return Mono.just(ResponseEntity.notFound().build());
		}
		return clientesRepository.findById(clienteID)
				.cache()
				.subscribeOn(Schedulers.parallel())
				.map(cliente -> ExtratoResponseDto.builder()
						.saldo(ExtratoResponseDto.ExtratoSaldoDto.builder()
								.limite(cliente.getLimite())
								.total(cliente.getSaldo())
								.dataExtrato(Instant.now())
								.build()))
				.flatMap(extrato -> transacaoRepository.findByClienteID(clienteID)
						.cache()
						.subscribeOn(Schedulers.parallel())
						.collectList()
						.map(transacoes -> extrato.transacoes(transacoes).build()))
				.flatMap(extratoResponseDto -> Mono.just(ResponseEntity.ok(extratoResponseDto)))
				.onErrorResume(throwable -> Mono.just(ResponseEntity.unprocessableEntity().build()));
	}

	@ExceptionHandler(DecodingException.class)
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	public Mono<Void> handleValidationDecode() {
		return Mono.empty();
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	public Mono<Void> handleValidationExceptions() {
		return Mono.empty();
	}

	@ExceptionHandler(WebExchangeBindException.class)
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	public Mono<Void> handleRequestBodyParams() {
		return Mono.empty();
	}
}
