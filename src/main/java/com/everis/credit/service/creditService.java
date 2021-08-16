package com.everis.credit.service;

import com.everis.credit.dto.*; 
import com.everis.credit.model.credit;
import com.everis.credit.model.creditCard;
import com.everis.credit.model.operation;
import com.everis.credit.webclient.webclient;

import com.everis.credit.repository.creditRepository;

import java.util.*; 
import reactor.core.publisher.*; 
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional; 

@Service
@Transactional
public class creditService {
  @Autowired
  creditRepository repository; //   

  private Boolean verifyCustomer(String id) {
    return webclient.customer
      .get()
      .uri("/verifyId/{id}", id)
      .retrieve()
      .bodyToMono(Boolean.class)
      .block();
  }

  private customer customerFind(String id) {
    return webclient.customer
      .get()
      .uri("/{id}", id)
      .retrieve()
      .bodyToMono(customer.class)
      .block();
  }

  private Boolean existsByNumberCreditCard(String number, String passowrd) {
	  Boolean x = false;
	  String code = webclient.logic
	      .get()
	      .uri("/encriptBySha1/" + passowrd)
	      .retrieve()
	      .bodyToMono(String.class)
	      .block();
	  
	  for (int i = 0; i < repository.findAll().size(); i++) {
		  creditCard card = repository.findAll().get(i).getCreditcard();
		  if ( card.getNumberCard().equals( number ) && card.getPassword().equals(code) ) {
			  x = true;
		  }
	  }
	  
	  return x;
  }

  private credit findByNumberCreditCard(String number, String passowrd) { 
	  String code = webclient.logic
		      .get()
		      .uri("/encriptBySha1/" + passowrd)
		      .retrieve()
		      .bodyToMono(String.class)
		      .block();
	  for (int i = 0; i < repository.findAll().size(); i++) { 
		  if ( repository.findAll().get(i).getCreditcard().getNumberCard().equals( number ) && repository.findAll().get(i).getCreditcard().getPassword().equals(code) ) {
			  return repository.findAll().get(i);
		  }
	  }
	  return null;

  }

  private String addOperations(
    credit creditObj,
    String type,
    operation obj,
    Double amount
  ) {
    double base = creditObj.getBaseCreditLimit();
    double baseB = creditObj.getAmount();
    String msg = "Monto pagado.";

    if (type.equals("consumo")) {
      if (amount > baseB) msg = "El monto sobre pasa el credito base."; else {
        creditObj.setAmount(baseB - amount);
        creditObj.getOperation().add(obj);
        repository.save(creditObj);
        msg = "Consumo añadido.";
      }
    } else {
      creditObj.setAmount(base);
      obj.setAmount(0);
      creditObj.getOperation().add(obj);
      repository.save(creditObj);
    }

    return msg;
  }

  public Mono<Object> save(String idAccount, double baseCreditLimit, String password) {
    String msg = "Credito registrado.";
    if (verifyCustomer(idAccount)) {
      credit obj = new credit(idAccount, baseCreditLimit, password);
      obj.setTypeAccount(customerFind(idAccount).getType());

      if (customerFind(idAccount).getType().equals("personal")) {
        if ( !repository.existsByIdCustomer(idAccount) ) repository.save(obj); else msg =
          "Usted ya no puede tener mas creditos.";
      } else repository.save(obj);
    } else msg = "Cliente no registrado";
    return Mono.just(new message(msg));
  }

  public Mono<Object> saveOperations(
    String numberCard,
    String password,
    Double amount,
    String type
  ) {
    String msg = "Operacion realizada.";

    if (existsByNumberCreditCard(numberCard, password)) {
      if (type.equals("consumo") || type.equals("pago")) msg =
        addOperations(
          findByNumberCreditCard(numberCard, password),
          type,
          new operation(amount, type),
          amount
        ); else msg = "Operacion incorrecta.";
    } else msg = "Datos incorrectos.";

    return Mono.just(new message(msg));
  }

 public Flux<credit> getByCustomer(String id) {
	  
	  List<credit> lista = repository.findAll();
	  List<credit> listb = new ArrayList<credit>();
	  
	  for (int i = 0; i < lista.size(); i++) {
		  if( lista.get(i).getIdCustomer().equals(id) ) {
			  listb.add(lista.get(i));
		  }
	  }
	  
	  	  
    return Flux.fromIterable(listb);
  }
  
  public Flux<credit> getAll(){
	return Flux.fromIterable( repository.findAll() );
  }
  
  public Mono<Boolean> _verifyCustomer(String id){ 
	  
	  for (int i = 0; i < repository.findAll().size(); i++) 
		  if( repository.findAll().get(i).getIdCustomer().equals(id) ) 
			  return Mono.just(true);
	  
	  	  
    return Mono.just(false);
  }
}
