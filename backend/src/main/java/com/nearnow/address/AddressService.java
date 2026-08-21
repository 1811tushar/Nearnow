package com.nearnow.address;

import com.nearnow.auth.User;
import com.nearnow.auth.UserRepository;
import com.nearnow.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    public List<AddressResponseDTO> getAddresses(String userEmail) {
        User user = getUser(userEmail);
        return addressRepository.findByUserId(user.getId()).stream().map(this::toDTO).toList();
    }

    @Transactional
    public AddressResponseDTO addAddress(String userEmail, AddressRequestDTO request) {
        User user = getUser(userEmail);

        // If this new address is being added as default, clear every
        // other address's default flag FIRST, in the same transaction —
        // same "only one default at a time" invariant as setDefaultAddress().
        if (request.isDefault()) {
            addressRepository.clearDefaultForUser(user.getId());
        }

        Address address = new Address(user, request.getLabel(), request.getFullName(), request.getPhone(),
                request.getAddressLine(), request.getCity(), request.getPincode(),
                request.getLatitude(), request.getLongitude(), request.isDefault());

        return toDTO(addressRepository.save(address));
    }

    @Transactional
    public AddressResponseDTO updateAddress(String userEmail, Long addressId, AddressRequestDTO request) {
        Address address = getOwnedAddress(userEmail, addressId);

        if (request.isDefault() && !address.isDefault()) {
            addressRepository.clearDefaultForUser(address.getUser().getId());
        }

        address.setLabel(request.getLabel());
        address.setFullName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setAddressLine(request.getAddressLine());
        address.setCity(request.getCity());
        address.setPincode(request.getPincode());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setDefault(request.isDefault());

        return toDTO(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(String userEmail, Long addressId) {
        Address address = getOwnedAddress(userEmail, addressId);
        addressRepository.delete(address);
    }

    // @Transactional here is the direct relational-equivalent of
    // AddressService.dart's Firestore batch.commit() — clearing every
    // address's default flag, then setting exactly one, must happen as
    // one atomic unit. If clearDefaultForUser() succeeded but the
    // following save() failed, the user would end up with ZERO default
    // addresses, silently — the transaction boundary prevents that.
    @Transactional
    public AddressResponseDTO setDefaultAddress(String userEmail, Long addressId) {
        Address address = getOwnedAddress(userEmail, addressId);
        addressRepository.clearDefaultForUser(address.getUser().getId());
        address.setDefault(true);
        return toDTO(addressRepository.save(address));
    }

    // Ownership check, same defensive pattern as CartService's
    // removeFromCart/updateQuantity — an address id alone doesn't prove
    // it belongs to the caller.
    private Address getOwnedAddress(String userEmail, Long addressId) {
        User user = getUser(userEmail);
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + addressId));
        if (!address.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Address not found: " + addressId);
        }
        return address;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private AddressResponseDTO toDTO(Address address) {
        return new AddressResponseDTO(address.getId(), address.getLabel(), address.getFullName(),
                address.getPhone(), address.getAddressLine(), address.getCity(), address.getPincode(),
                address.getLatitude(), address.getLongitude(), address.isDefault());
    }
}
