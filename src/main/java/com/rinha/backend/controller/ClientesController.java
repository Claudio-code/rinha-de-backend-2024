package com.rinha.backend.controller;

import com.rinha.backend.dto.ExtratoResponseDto;
import com.rinha.backend.entity.Clientes;
import com.rinha.backend.entity.Transacoes;
import com.rinha.backend.exceptions.SaldoInconsistenteException;
import com.rinha.backend.repository.ClientesRepository;
import com.rinha.backend.repository.TransacoesPageableRepository;
import com.rinha.backend.repository.TransacoesRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.codec.DecodingException;
import org.springframework.data.domain.PageRequest;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping("clientes")
public class ClientesController {
	private final ClientesRepository clientesRepository;
	private final TransacoesRepository transacaoRepository;
	private final TransacoesPageableRepository transacoesPageableRepository;

	@PostMapping("{id}/transacoes")
	@Transactional(isolation = Isolation.READ_COMMITTED)
	public Mono<ResponseEntity<TransacaoResponseDto>> fazerTransacao(
			@PathVariable("id") @Valid final Integer clienteID,
			@RequestBody @Valid final TransacaoRequestDto transacaoDto) {
		if (clienteID < 1 || clienteID > 5) {
			return Mono.just(ResponseEntity.notFound().build());
		}
		final var trasacaoPublisher = transacaoRepository.save(Transacoes.builder()
				.clienteID(clienteID)
				.tipo(transacaoDto.tipo())
				.descricao(transacaoDto.descricao())
				.valor(transacaoDto.valor())
				.realizadaEm(Instant.now())
				.build());
		return clientesRepository.findById(clienteID)
				.flatMap(cliente -> {
					if (transacaoDto.tipo().equals("c")) {
						cliente.adicionarSaldo(transacaoDto.valor());
					} else {
						cliente.removerSaldo(transacaoDto.valor());
					}
					if (transacaoDto.valor() > (cliente.getSaldo() + cliente.getLimite())) {
						return Mono.error(new SaldoInconsistenteException());
					}
					return Mono.just(cliente);
				})
				.flatMap(clientesRepository::save)
				.flatMap(trasacaoPublisher::thenReturn)
				.flatMap(cliente -> Mono
						.just(ResponseEntity.ok(new TransacaoResponseDto(cliente.getLimite(), cliente.getSaldo()))))
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
				.flatMap(extrato ->
						transacoesPageableRepository.findByClienteID(clienteID, PageRequest.of(0, 10))
						.cache()
						.subscribeOn(Schedulers.parallel())
						.collectList()
						.map(transacoes -> extrato.transacoes(transacoes).build()))
				.flatMap(extratoResponseDto -> Mono.just(ResponseEntity.ok(extratoResponseDto)))
				.onErrorResume(throwable -> Mono.just(ResponseEntity.unprocessableEntity().build()));
	}

	public record TransacaoRequestDto(
			@NotNull  @Min(0) Integer valor,
			@NotNull @NotEmpty @Pattern(regexp = "[c|d]") String tipo,
			@NotNull @NotEmpty @NotBlank @Length(max = 10) String descricao) {
	}

	public record TransacaoResponseDto(Integer limite, Integer saldo) {
	}


	@ExceptionHandler({DecodingException.class, MethodArgumentNotValidException.class, WebExchangeBindException.class})
	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	public Mono<Void> handleValidationDecode() {
		return Mono.empty();
	}
}
