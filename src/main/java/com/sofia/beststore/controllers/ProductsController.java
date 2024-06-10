package com.sofia.beststore.controllers;
import com.sofia.beststore.models.Product;
import com.sofia.beststore.models.ProductDto;
import com.sofia.beststore.services.ProductsRepository;
// Demasiadas importaciones .__.

import jakarta.validation.Valid;

import java.io.InputStream;
import java.nio.file.*;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/products")
public class ProductsController {

    @Autowired
    private ProductsRepository repo;


    @GetMapping({"", "/"}) // estará disponible tanto para como "products/" o "products"
    public String showProductsList(Model model){ // metodo que va a mostrar la lista de productos
        List<Product> products = repo.findAll(Sort.by(Sort.Direction.DESC, "id") ); // almacena los productos en una lista (llama al metetodo del jpa)
        // el sort hace que se ordene de menor a mayor
        model.addAttribute("products", products); // se agrega un atributo llamado "products" que va a contener dicha lista
        return "products/index"; // la vista que se va a renderizar en la carpeta templlates/products/index
    }

    // este es para retornar el formulario para crear nuevos productos
    @GetMapping("/create")
    public String showCreatePage(Model model){
        ProductDto productDto = new ProductDto();
        model.addAttribute("productDto", productDto);
        return "products/createProduct";
    }

    // metodo POST
    @PostMapping("/create")
    public String createProduct(
        @Valid @ModelAttribute ProductDto productDto, //manda el objeto validado
        BindingResult result  //manda el resultado
    ){
        // validación a la imagen (porque no se valido en productDto)
        if (productDto.getImageFile().isEmpty()){
            result.addError(new FieldError("productDto", "imageFile", "The image file is missing"));
        }

        if (result.hasErrors()){ // si hay errores, retorna a la pagina principal
            return "products/createProduct";
        }
        

        // save image file
        MultipartFile image = productDto.getImageFile(); //obtener la imagen 
        Date createdAt = new Date(); // crea una instancia de date
        String storageFileName = createdAt.getTime() + "_" + image.getOriginalFilename (); //generacion de nombre unico

        try {
            String uploadDir = "public/images/";
            Path uploadPath = Paths.get(uploadDir); // se comprueba la carpeta de carga
            
            if (!Files.exists(uploadPath)) { // ¿Existe la carpeta de carga?
                Files.createDirectories(uploadPath); //Si no existe, se crea
            }
            
            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(inputStream, Paths.get(uploadDir + storageFileName), // se copia al directorio de carga  
                StandardCopyOption.REPLACE_EXISTING);
            }
            
        } catch (Exception ex) { //manejo de errores
            System.out.println("Exception: "+ ex.getMessage());
        }


        // creacion de nuevo objeto producto
        Product product = new Product (); 
        product.setName(productDto.getName()); 
        product.setBrand(productDto.getBrand());
        product.setCategory(productDto.getCategory());
        product.setPrice(productDto.getPrice());
        product.setDescription(productDto.getDescription());
        product.setCreatedAt(createdAt);
        product.setImageFileName(storageFileName);


        repo.save(product); // se guarda el producto

        return "redirect:/products"; // redirige a productos despues de crear uno nuevo
    }

    /*---- EDICIION*/
    @GetMapping("/edit")
    public String showEditPage(
        Model model, //objeto
        @RequestParam int id //id del objeto extraido de la url
    ){

        try{
            Product product = repo.findById(id).get(); // busca el producto por su id (parametro) y recupera el objeto
            model.addAttribute("product", product); 

            // crear nuevo producto
            ProductDto productDto = new ProductDto(); productDto.setName(product.getName());
            productDto.setBrand (product.getBrand());
            productDto.setCategory (product.getCategory());
            productDto.setPrice (product.getPrice());
            productDto.setDescription (product.getDescription());

            model.addAttribute("productDto", productDto);
        }
        catch(Exception ex){
            System.out.println("Exception: " + ex.getMessage());
            return "redirect:/products";
        }

        return "products/editProduct"; //retorna  a la vista de edicion en plantillas
    }


    /*--- EDITAR  LA DATA DEL POSTEO ---*/
    @PostMapping("/edit")
    public String updateProduct(
        Model model, //recibe objeto
        @RequestParam int id, //recibe el id del parametro
        @Valid @ModelAttribute ProductDto productDto,  //valida los datos   
        BindingResult result //captura errores
    ){

        try{
            // recupera el objeto producto
            Product product = repo.findById(id).get();
            model.addAttribute("product", product);

            if (result.hasErrors()){ // si hay errores
                return "products/editProduct";
            }

            if (!productDto.getImageFile().isEmpty()) { 
                // delete old image
                String uploadDir = "public/images/";
                Path oldImagePath = Paths.get(uploadDir + product.getImageFileName());

                try {
                    Files.delete(oldImagePath);
                } catch (Exception ex) {
                    System.out.println("Exception: " + ex.getMessage());
                }
                // save new image file	
                MultipartFile image = productDto.getImageFile();
                Date createdAt = new Date();
                String storageFileName = createdAt.getTime() + "_"  + image.getOriginalFilename();

                try (InputStream inputStream = image.getInputStream()) { 
                    Files.copy(inputStream, Paths.get(uploadDir + storageFileName), //se sube la imagen a un nuevo path
                    StandardCopyOption.REPLACE_EXISTING);
                }

                product.setImageFileName(storageFileName); // guarda la nueva imagen 
            }

            // actualiza los otros detalles del producto
            product.setName(productDto.getName());
            product.setBrand(productDto.getBrand());
            product.setCategory(productDto.getCategory());
            product.setPrice(productDto.getPrice());
            product.setDescription(productDto.getDescription());

            repo.save(product);

        } catch(Exception ex){
            System.out.println("Exception: " + ex.getMessage()); // impresion del mensaje de error
        }
        return "redirect:/products";
    }

    // ELIMINAR PRODUCTOS 

    @GetMapping("/delete")
    public String deleteProduct(
        @RequestParam int id
    ){  
        try{
            Product product = repo.findById(id).get();

            // borrar la imagen del producto
            Path imagePath = Paths.get("public/images/" + product.getImageFileName());

            try{
                Files.delete(imagePath);

            } catch (Exception ex){
                System.out.println("Exception " + ex.getMessage());
            }

           repo.delete(product); //eliminar el producto desde la base de datos

        }catch(Exception ex){
            System.out.println("Exception: " +  ex.getMessage());
        }
        return "redirect:/products";
    }


}
