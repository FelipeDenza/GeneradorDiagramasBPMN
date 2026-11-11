package com.desafiotecnicoimr.bpmn.presentation;

import com.desafiotecnicoimr.bpmn.presentation.dto.GenerateDiagramRequest;
import com.desafiotecnicoimr.bpmn.presentation.dto.GenerateDiagramResponse;
import com.desafiotecnicoimr.bpmn.application.GenerateDiagramUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/diagrams")
public class DiagramController {

    private final GenerateDiagramUseCase generateDiagram;

    public DiagramController(GenerateDiagramUseCase generateDiagram) {
        this.generateDiagram = generateDiagram;
    }

    @PostMapping("/generate")
    public GenerateDiagramResponse generate(@Valid @RequestBody GenerateDiagramRequest request) {
        return GenerateDiagramResponse.from(generateDiagram.generate(request.prompt(), request.provider()));
    }
}
