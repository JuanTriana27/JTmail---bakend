package co.jtmail.service.impl;

import co.jtmail.dto.request.CreateUserRequest;
import co.jtmail.dto.request.UpdateUserRequest;
import co.jtmail.dto.response.UserResponse;
import co.jtmail.exception.ConflictException;
import co.jtmail.exception.ResourceNotFoundException;
import co.jtmail.mapper.UserMapper;
import co.jtmail.model.User;
import co.jtmail.repository.UserRepository;
import co.jtmail.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    // Listar todos los usuarios
    @Override
    public List<UserResponse> getAllUsers(){
        return userRepository.findAll()
                .stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    // Buscar por id
    @Override
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: ", id));

        return UserMapper.toResponse(user);
    }

    // Crear Usuario
    @Override
    public UserResponse createUser(CreateUserRequest request) {

        // Validación de negocio
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("El email ya está registrado");
        }

        // Mapear request → entity
        User user = UserMapper.toEntity(request);

        // Aquí luego irá el hash de contraseña (BCrypt)
        // user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        // Guardar
        user = userRepository.save(user);

        // Retornar response
        return UserMapper.toResponse(user);
    }

    // ← cambia CreateUserRequest por UpdateUserRequest
    @Override
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id)); // ← excepción correcta

        user.setFullName(request.getFullName());
        user.setAvatarUrl(request.getAvatarUrl());
        // Email no se toca en update

        return UserMapper.toResponse(userRepository.save(user));
    }

    // Eliminar Usuaro
    @Override
    public void deleteUser(UUID id) {

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("No existe usuario con id: ", id);
        }

        userRepository.deleteById(id);
    }
}