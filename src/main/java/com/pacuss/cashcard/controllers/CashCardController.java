package com.pacuss.cashcard.controllers;

import com.pacuss.cashcard.model.CashCard;
import com.pacuss.cashcard.repository.CashCardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/cash_cards")
public class CashCardController {

    @Autowired
    private CashCardRepository cashCardRepository;

    // Get a single cashCard by id and principal
    @GetMapping("/{requestedId}")
    private ResponseEntity<CashCard> findById(@PathVariable Long requestedId, Principal principal){
        CashCard cashCard = findCashCard(requestedId,principal);

        if (cashCard == null) return ResponseEntity.notFound().build();
        else return ResponseEntity.ok(cashCard);
    }

    // Get multiple cashCards belonging to a user using pagination
    // cash_cards?page=1&size=3&sort=amount,desc
    // page index for the second page - indexing starts at 0. Default page is 0
    // page size (the last page might have fewer items). Default size is 20
    @GetMapping
    private ResponseEntity<List<CashCard>> findAll(Pageable pageable, Principal principal){
        Page<CashCard> page = cashCardRepository.findByOwner(
                principal.getName(),
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        pageable.getSortOr(Sort.by(Sort.Direction.DESC, "amount"))
                )
        );
        return ResponseEntity.ok(page.getContent());
    }

    // Post a single cashCard to the db
    // Ensure that only the authenticated, authorized Principal owns the CashCards they are creating.
    @PostMapping
    private ResponseEntity<Void> createCashCard(@RequestBody CashCard newCashCardRequest, UriComponentsBuilder ucb,
                                                             Principal principal){

        CashCard cashCardWithOwner = new CashCard(null, newCashCardRequest.amount(), principal.getName());
        CashCard savedCashCard = cashCardRepository.save(cashCardWithOwner);

        URI locationOfNewCashCard = ucb
                .path("cash_cards/{id}")
                .buildAndExpand(savedCashCard.id())
                .toUri();

        return ResponseEntity.created(locationOfNewCashCard).build();
    }

    // Update a single cashCard
    @PutMapping("/{requestedId}")
    private ResponseEntity<Void> putCashCard(@PathVariable Long requestedId,
                                             @RequestBody CashCard cashCardUpdate, Principal principal){

        CashCard cashCard = findCashCard(requestedId,principal);
        if (cashCard == null) return ResponseEntity.notFound().build();

        CashCard updatedCashCard = new CashCard(cashCard.id(), cashCardUpdate.amount(), principal.getName());
        cashCardRepository.save(updatedCashCard);
        return ResponseEntity.noContent().build();
    }

    // Update a single cashCard
    @DeleteMapping("/{requestedId}")
    private ResponseEntity<Void> deleteCashCard(@PathVariable Long requestedId, Principal principal){

        if (cashCardRepository.existsByIdAndOwner(requestedId, principal.getName())){
            cashCardRepository.deleteById(requestedId);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Helper method to find a cashCard
    private CashCard findCashCard(Long requestedId, Principal principal) {
        return cashCardRepository.findByIdAndOwner(requestedId, principal.getName());
    }
}
