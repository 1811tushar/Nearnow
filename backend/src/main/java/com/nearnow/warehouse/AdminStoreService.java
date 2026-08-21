package com.nearnow.warehouse;

import com.nearnow.auth.User;
import com.nearnow.auth.UserRepository;
import com.nearnow.common.exception.InvalidOperationException;
import com.nearnow.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

@Service
public class AdminStoreService {
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    public AdminStoreService(StoreRepository storeRepository, UserRepository userRepository){this.storeRepository=storeRepository;this.userRepository=userRepository;}

    public Page<StoreResponseDTO> getStores(Pageable pageable){return storeRepository.findAll(pageable).map(this::toDto);}

    @Transactional
    public StoreResponseDTO save(Long id, StoreRequestDTO r){
        validateCoordinates(r.getLatitude(),r.getLongitude());
        Store store=id==null?new Store(r.getName().trim(),r.getAddressLine().trim(),r.getCity().trim(),r.getPincode().trim(),r.getLatitude(),r.getLongitude(),r.getCapacity(),parseTime(r.getOperatingHoursStart()),parseTime(r.getOperatingHoursEnd())):storeRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Store not found"));
        store.setName(r.getName().trim()); store.setAddressLine(r.getAddressLine().trim()); store.setCity(r.getCity().trim()); store.setPincode(r.getPincode().trim()); store.setLatitude(r.getLatitude()); store.setLongitude(r.getLongitude()); store.setCapacity(r.getCapacity()); store.setOperatingHoursStart(parseTime(r.getOperatingHoursStart())); store.setOperatingHoursEnd(parseTime(r.getOperatingHoursEnd())); store.setActive(true);
        assignManager(store,r.getWarehouseManagerUserId());
        return toDto(storeRepository.save(store));
    }

    @Transactional public StoreResponseDTO setActive(Long id, boolean active){Store s=storeRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Store not found"));s.setActive(active);return toDto(storeRepository.save(s));}

    private void assignManager(Store store, Long userId){
        if(userId==null){store.setWarehouseManager(null);return;}
        User user=userRepository.findById(userId).orElseThrow(()->new ResourceNotFoundException("Warehouse manager user not found"));
        if(!"warehouse_manager".equalsIgnoreCase(user.getRole())) throw new InvalidOperationException("User must have role=warehouse_manager");
        storeRepository.findByWarehouseManagerId(userId).ifPresent(existing -> {
            if (existing.getId() != null && !existing.getId().equals(store.getId()) && existing.isActive()) {
                throw new InvalidOperationException("This warehouse manager is already assigned to another active store");
            }
        });
        store.setWarehouseManager(user);
    }
    private LocalTime parseTime(String value){return value==null||value.isBlank()?null:LocalTime.parse(value);}
    private void validateCoordinates(double lat,double lon){if(lat < -90 || lat > 90 || lon < -180 || lon > 180) throw new InvalidOperationException("Invalid store coordinates");}
    private StoreResponseDTO toDto(Store s){User m=s.getWarehouseManager();return new StoreResponseDTO(s.getId(),s.getName(),s.getAddressLine(),s.getCity(),s.getPincode(),s.getLatitude(),s.getLongitude(),s.getCapacity(),s.getOperatingHoursStart()==null?null:s.getOperatingHoursStart().toString(),s.getOperatingHoursEnd()==null?null:s.getOperatingHoursEnd().toString(),s.isActive(),m==null?null:m.getId(),m==null?null:m.getEmail());}
}
