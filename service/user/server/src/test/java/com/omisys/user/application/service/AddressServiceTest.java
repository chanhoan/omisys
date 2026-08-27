package com.omisys.user.application.service;

import com.omisys.user.domain.model.Address;
import com.omisys.user.domain.model.User;
import com.omisys.user.domain.repository.AddressRepository;
import com.omisys.user.domain.repository.UserRepository;
import com.omisys.user.exception.UserErrorCode;
import com.omisys.user.exception.UserException;
import com.omisys.user.presentation.request.AddressRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AddressRepository addressRepository;

    @InjectMocks private AddressService addressService;

    @Test
    @DisplayName("createAddress 성공: user 존재 → addressRepository.save 호출")
    void createAddress_success() {
        // given
        long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(User.class)));

        AddressRequest.Create request = mock(AddressRequest.Create.class);

        // when
        addressService.createAddress(userId, request);

        // then
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    @DisplayName("createAddress 실패: user 없으면 USER_NOT_FOUND 예외")
    void createAddress_fail_user_not_found() {
        // given
        long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        AddressRequest.Create request = mock(AddressRequest.Create.class);

        // when & then
        assertThatThrownBy(() -> addressService.createAddress(userId, request))
                .isInstanceOf(UserException.class)
                .satisfies(ex -> {
                    UserException ue = (UserException) ex;
                    assertThat(ue.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
                });

        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateAddress 성공: user 존재 + address 존재 → address.update 호출")
    void updateAddress_success() {
        // given
        long userId = 1L;
        long addressId = 10L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(User.class)));

        Address address = mock(Address.class);
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

        AddressRequest.Update request = mock(AddressRequest.Update.class);

        // when
        addressService.updateAddress(userId, addressId, request);

        // then
        verify(address).update(request);
    }

    @Test
    @DisplayName("deleteAddress 성공: user 존재 + address 존재 → addressRepository.delete 호출")
    void deleteAddress_success() {
        // given
        long userId = 1L;
        long addressId = 10L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(User.class)));

        Address address = mock(Address.class);
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

        // when
        addressService.deleteAddress(userId, addressId);

        // then
        verify(addressRepository).delete(address);
    }
}
