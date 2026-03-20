package com.finup.pessoaFisica;

import com.finup.auth.AuthService;
import com.finup.pessoaFisica.dto.DetailPessoaFisicaResponse;
import com.finup.pessoaFisica.dto.UpdatePessoaFisicaRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pessoaFisica")
@SecurityRequirement(name = "bearer-key")
public class PessoaFisicaController {


}
