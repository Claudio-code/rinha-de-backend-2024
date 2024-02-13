package com.rinha.backend.controller;

import java.time.Instant;
import java.util.Objects;

import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.support.WebExchangeBindException;

import com.rinha.backend.dto.ExtratoResponseDto;
import com.rinha.backend.dto.TransacaoRequestDto;
import com.rinha.backend.dto.TransacaoResponseDto;
import com.rinha.backend.entity.Transacoes;
import com.rinha.backend.enums.TipoTransacao;
import com.rinha.backend.exceptions.SaldoInconsistenteException;
import com.rinha.backend.exceptions.TipoErradoException;
import com.rinha.backend.repository.ClientesRepository;
import com.rinha.backend.repository.TransacoesRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequiredArgsConstructor
@RequestMapping("clientes")
public class ClientesController {
    private final ClientesRepository clientesRepository;
	private final TransacoesRepository transacaoRepository;

	@PostMapping("{id}/transacoes")
	@Transactional
	public Mono<TransacaoResponseDto> fazerTransacao(
			@PathVariable("id") @Valid final Integer clienteID,
			@RequestBody @Valid final TransacaoRequestDto transacaoDto)
			throws TipoErradoException, ChangeSetPersister.NotFoundException {
		if (clienteID < 1 || clienteID > 5) {
			throw new ChangeSetPersister.NotFoundException();
		}
		if ((!TipoTransacao.isValidType(transacaoDto.tipo())) ||
				transacaoDto.tipo().chars().count() > 1 ||
				transacaoDto.descricao().chars().count() > 10) {
			throw new TipoErradoException();
		}

		final var transacoes = Transacoes.builder()
				.clienteID(clienteID)
				.tipo(transacaoDto.tipo())
				.descricao(transacaoDto.descricao())
				.valor(transacaoDto.valor())
				.realizadaEm(Instant.now())
				.build();
		final var trasacaoPublisher = transacaoRepository.save(transacoes).subscribeOn(Schedulers.parallel());
		return clientesRepository.findById(clienteID)
				.subscribeOn(Schedulers.parallel())
				.switchIfEmpty(Mono.error(new ChangeSetPersister.NotFoundException()))
				.flatMap(cliente -> {
					if (Objects.equals(TipoTransacao.CREDITO.getTipo(), transacaoDto.tipo())) {
						cliente.adicionarSaldo(transacaoDto.valor());
					} else {
						cliente.removerSaldo(transacaoDto.valor());
					}
					if (cliente.verificarSeSaldoEstaInconsistente()) {
						return Mono.error(new SaldoInconsistenteException());
					}
					return Mono.just(cliente);
				})
				.flatMap(clientesRepository::save)
				.flatMap(trasacaoPublisher::thenReturn)
				.flatMap(cliente -> Mono.just(new TransacaoResponseDto(cliente.getLimite(), cliente.getSaldo())));
	}

	@GetMapping("{id}/extrato")
	public Mono<ExtratoResponseDto> extrato(@PathVariable("id") @Valid final Integer clienteID)
			throws ChangeSetPersister.NotFoundException {
		if (clienteID < 1 || clienteID > 5) {
			throw new ChangeSetPersister.NotFoundException();
		}
		return clientesRepository.findById(clienteID)
				.cache()
				.subscribeOn(Schedulers.parallel())
				.switchIfEmpty(Mono.error(new ChangeSetPersister.NotFoundException()))
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
						.map(transacoes -> extrato.transacoes(transacoes).build()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	public Mono<Void> handleValidationExceptions() {
		return Mono.empty();
	}

	@ExceptionHandler(ChangeSetPersister.NotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public Mono<Void> handleNotfound() {
		return Mono.empty();
	}

	@ExceptionHandler(TipoErradoException.class)
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	public Mono<Void> handleTransactionErrorType() {
		return Mono.empty();
	}

	@ExceptionHandler(SaldoInconsistenteException.class)
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	public Mono<Void> handleInconsistentBalance() {
		return Mono.empty();
	}

	@ExceptionHandler(WebExchangeBindException.class)
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	public Mono<Void> handleRequestBodyParams() {
		return Mono.empty();
	}
}
