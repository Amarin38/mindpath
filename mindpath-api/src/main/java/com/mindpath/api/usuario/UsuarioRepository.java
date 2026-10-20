package com.mindpath.api.usuario;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {
    List<Usuario> findByEmail(String email);
}
