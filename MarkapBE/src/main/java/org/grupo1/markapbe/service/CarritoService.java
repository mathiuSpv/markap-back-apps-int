package org.grupo1.markapbe.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.grupo1.markapbe.controller.dto.CarritoDTO;
import org.grupo1.markapbe.controller.dto.ItemsCarritoDTO;
import org.grupo1.markapbe.persistence.entity.CarritoEntity;
import org.grupo1.markapbe.persistence.entity.ItemsCarritoEntity;
import org.grupo1.markapbe.persistence.entity.ProductEntity;
import org.grupo1.markapbe.persistence.entity.UserEntity;
import org.grupo1.markapbe.persistence.repository.CarritoRepository;
import org.grupo1.markapbe.persistence.repository.ItemsCarritoRepository;
import org.grupo1.markapbe.persistence.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
@Service
public class CarritoService {

    @Autowired
    private CarritoRepository carritoRepository;

    @Autowired
    private ItemsCarritoRepository itemsCarritoRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Creates a new cart for the current user.
     *
     * @return CarritoEntity The entity representation of the newly created cart.
     */
    public CarritoEntity createCarrito() {
        UserEntity user = userService.obtenerUsuarioPeticion();
        Optional<CarritoEntity> actualCarrito = carritoRepository.findActiveCarritoByUser(user.getId());

        if (actualCarrito.isPresent()) {
            throw new IllegalStateException("El usuario ya tiene un Carrito Activo");
        }

        CarritoEntity newCarrito = CarritoEntity.builder()
                .User(user)
                .build();
        return carritoRepository.save(newCarrito);
    }

    /**
     * Creates a new cart for the current user and returns a DTO.
     *
     * @return CarritoDTO The DTO representation of the newly created cart.
     */
    public CarritoDTO createCarritoDTO() {
        CarritoEntity carrito = createCarrito();
        return convertToDTO(carrito);
    }

    /**
     * Retrieves the active cart (paymentStatus = false) for the current user.
     *
     * @return CarritoEntity The entity representation of the active cart.
     */
    public CarritoEntity getActiveCarrito() {
        UserEntity user = userService.obtenerUsuarioPeticion();
        return carritoRepository.findActiveCarritoByUser(user.getId())
                .orElse(null);
    }

    /**
     * Retrieves the active cart (paymentStatus = false) for the current user.
     *
     * @return Optional<CarritoDTO> The Optional DTO representation of the active cart.
     */
    public Optional<CarritoDTO> getActiveCarritoDTO() {
        return Optional.of(convertToDTO(getActiveCarrito()));
    }

    /**
     * Retrieves all items from a specific cart.
     *
     * @param carritoId The ID of the cart.
     * @return Set<ItemsCarritoEntity> A set of entity representations of the items in the cart.
     */
    public Set<ItemsCarritoEntity> getAllItemsByCarrito(Long carritoId) {
        return itemsCarritoRepository.findAllItemsByCarritoId(carritoId)
                .orElseThrow(() -> new EntityNotFoundException("Item carrito no encontrado."));
    }

    /**
     * Retrieves all items from a specific cart and returns Optional DTOs.
     *
     * @param carritoDTO The DTO representation of the cart.
     * @return Optional<Set<ItemsCarritoDTO>> A set of Optional DTOs representing the items in the cart.
     */
    public Optional<Set<ItemsCarritoDTO>> getAllItemsByCarritoDTO(CarritoDTO carritoDTO) {
        Long carritoId = carritoDTO.id();
        return Optional.of(getAllItemsByCarrito(carritoId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toSet()));
    }

    /**
     * Retrieves a cart entity by its ID.
     *
     * @param carritoId The ID of the cart.
     * @return CarritoEntity The entity representation of the cart.
     */
    public CarritoEntity getCarrito(Long carritoId) {
        return carritoRepository.findById(carritoId)
                .orElseThrow(() -> new EntityNotFoundException("Carrito no encontrado."));
    }

    /**
     * Retrieves a cart DTO by its ID.
     *
     * @param carritoId The ID of the cart.
     * @return Optional<CarritoDTO> The Optional DTO representation of the cart.
     */
    public Optional<CarritoDTO> getCarritoDTO(Long carritoId) {
        return Optional.of(convertToDTO(getCarrito(carritoId)));
    }

    /**
     * Retrieves all carts for the current user.
     *
     * @return Optional<List<CarritoDTO>> A list of Optional DTOs representing all carts.
     */
    public Optional<List<CarritoDTO>> getAllCarritos() {
        UserEntity user = userService.obtenerUsuarioPeticion();
        List<CarritoEntity> allCarritos = carritoRepository.findAllByUser(user.getId())
                .orElseThrow(() -> new IllegalStateException("Debe existir al menos el carrito Activo"));
        return Optional.of(allCarritos.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));
    }

    // ACCIONES

    /**
     * Changes the status of a cart to 'paid' (paymentStatus = true).
     *
     * @return CarritoDTO The updated DTO representation of the cart.
     */
    public CarritoDTO changeStatusCarritoToPaid() {
        CarritoEntity carrito = getActiveCarrito();
        carrito.setPaymentStatus(true);
        return convertToDTO(carritoRepository.save(carrito));
    }

    /**
     * Adds an item to the cart. If the item exists, increments the amount.
     * If the item does not exist, creates a new item in the cart.
     *
     * @param productId The ID of the product to be added to the cart.
     * @param amount The amount of the product to add to the cart.
     * @return ItemsCarritoDTO The DTO representation of the item added or updated.
     */
    public ItemsCarritoDTO addItemToCarrito(Long productId, int amount) {
        CarritoEntity carrito = getActiveCarrito();
        Optional<ItemsCarritoEntity> itemCarrito = itemsCarritoRepository.findByCarritoIdAndProductId(
                carrito.getId(), productId);

        if (itemCarrito.isEmpty()) {
            ProductEntity product = productRepository.findById(productId)
                    .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado."));

            ItemsCarritoEntity newItemCarrito = ItemsCarritoEntity.builder()
                    .carrito(carrito)
                    .product(product)
                    .amount(amount)
                    .build();
            return convertToDTO(itemsCarritoRepository.save(newItemCarrito));
        }

        ItemsCarritoEntity itemCarritoEnt = itemCarrito.get();
        itemCarritoEnt.setAmount(itemCarritoEnt.getAmount() + amount);
        return convertToDTO(itemsCarritoRepository.save(itemCarritoEnt));
    }

    /**
     * Removes an item from the cart. If the amount is 1, the item is deleted.
     * Otherwise, the amount is decremented by the specified amount.
     *
     * @param productId The ID of the product to be removed from the cart.
     * @param amount The amount to decrement from the cart item.
     * @return ItemsCarritoDTO The DTO representation of the updated item, or null if deleted.
     */
    public ItemsCarritoDTO removeItemFromCarrito(Long productId, int amount) {
        CarritoEntity carrito = getActiveCarrito();
        ItemsCarritoEntity itemCarrito = itemsCarritoRepository.findByCarritoIdAndProductId(
                        carrito.getId(), productId)
                .orElseThrow(() -> new EntityNotFoundException("Item del Carrito no encontrado."));

        if (itemCarrito.getAmount() < amount) {
            throw new IllegalArgumentException("La cantidad a eliminar del Producto no puede ser mayor a lo ya existente en el Carrito");
        }
        if (itemCarrito.getAmount() == amount) {
            itemsCarritoRepository.delete(itemCarrito);
            return null;
        }

        itemCarrito.setAmount(itemCarrito.getAmount() - amount);
        return convertToDTO(itemsCarritoRepository.save(itemCarrito));
    }

    /**
     * Converts CarritoEntity to CarritoDTO using ObjectMapper.
     *
     * @param carritoEntity The cart entity to be converted.
     * @return CarritoDTO The DTO representation of the cart.
     */
    private CarritoDTO convertToDTO(CarritoEntity carritoEntity) {
        return objectMapper.convertValue(carritoEntity, CarritoDTO.class);
    }

    /**
     * Converts ItemsCarritoEntity to ItemsCarritoDTO using ObjectMapper.
     *
     * @param itemsCarritoEntity The cart item entity to be converted.
     * @return ItemsCarritoDTO The DTO representation of the cart item.
     */
    private ItemsCarritoDTO convertToDTO(ItemsCarritoEntity itemsCarritoEntity) {
        return objectMapper.convertValue(itemsCarritoEntity, ItemsCarritoDTO.class);
    }
}