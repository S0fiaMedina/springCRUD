package com.sofia.beststore.services;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sofia.beststore.models.Product;

public interface ProductsRepository extends JpaRepository<Product, Integer>{ // recibe un producto y su llave primaria


    
}
