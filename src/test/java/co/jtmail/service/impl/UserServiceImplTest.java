package co.jtmail.service.impl;

import co.jtmail.dto.request.CreateUserRequest;
import co.jtmail.dto.request.UpdateUserRequest;
import co.jtmail.dto.response.UserResponse;
import co.jtmail.exception.ConflictException;
import co.jtmail.exception.ResourceNotFoundException;
import co.jtmail.model.User;
import co.jtmail.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User mockUser;
    private UUID userId;

    // Se ejecuta antes de cada test — evita repetir el setup
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockUser = User.builder()
                .idUser(userId)
                .email("juan@test.com")
                .fullName("Juan Torres")
                .isActive(true)
                .unreadCount(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // ─── getAllUsers ───────────────────────────────────────────

    @Test
    void getAllUsers_retornaListaDeUsuarios() {
        when(userRepository.findAll()).thenReturn(List.of(mockUser));

        List<UserResponse> result = userService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("juan@test.com");
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getAllUsers_retornaListaVaciaSiNoHayUsuarios() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserResponse> result = userService.getAllUsers();

        assertThat(result).isEmpty();
    }

    // ─── getUserById ───────────────────────────────────────────

    @Test
    void getUserById_retornaUsuarioCuandoExiste() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        UserResponse result = userService.getUserById(userId);

        assertThat(result.getIdUser()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo("juan@test.com");
    }

    @Test
    void getUserById_lanzaExcepcionCuandoNoExiste() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Verifica que lanza la excepción correcta, no un RuntimeException genérico
        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── createUser ────────────────────────────────────────────

    @Test
    void createUser_creaYRetornaUsuario() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("nuevo@test.com")
                .fullName("Nuevo Usuario")
                .password("password123")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserResponse result = userService.createUser(request);

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_lanzaConflictCuandoEmailDuplicado() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("juan@test.com")
                .fullName("Otro")
                .password("password123")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("email");

        // Verifica que nunca llegó a intentar guardar
        verify(userRepository, never()).save(any());
    }

    // ─── updateUser ────────────────────────────────────────────

    @Test
    void updateUser_actualizaYRetornaUsuario() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .fullName("Juan Torres Actualizado")
                .avatarUrl("https://ejemplo.com/foto.jpg")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserResponse result = userService.updateUser(userId, request);

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_lanzaExcepcionCuandoNoExiste() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .fullName("No importa")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(userId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        // Confirma que nunca intentó guardar un usuario inexistente
        verify(userRepository, never()).save(any());
    }

    // ─── deleteUser ────────────────────────────────────────────

    @Test
    void deleteUser_eliminaCuandoExiste() {
        when(userRepository.existsById(userId)).thenReturn(true);

        userService.deleteUser(userId);

        verify(userRepository).deleteById(userId);
    }

    @Test
    void deleteUser_lanzaExcepcionCuandoNoExiste() {
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(userId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository, never()).deleteById(any());
    }
}