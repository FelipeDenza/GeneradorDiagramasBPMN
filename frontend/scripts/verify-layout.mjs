import assert from 'node:assert/strict';
import { layoutProcess } from 'bpmn-auto-layout';

const semanticBpmnXml = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions
  xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  id="Definitions_1"
  targetNamespace="https://desafiotecnicoimr.com/bpmn">
  <bpmn:process id="Process_1" name="Sample process" isExecutable="false">
    <bpmn:startEvent id="StartEvent_1" name="Start">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:task id="Task_1" name="Review request">
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
    </bpmn:task>
    <bpmn:endEvent id="EndEvent_1" name="End">
      <bpmn:incoming>Flow_2</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="Task_1" />
    <bpmn:sequenceFlow id="Flow_2" sourceRef="Task_1" targetRef="EndEvent_1" />
  </bpmn:process>
</bpmn:definitions>`;

const laidOutXml = await layoutProcess(semanticBpmnXml);

assert.match(laidOutXml, /BPMNDiagram/);
assert.match(laidOutXml, /BPMNShape/);
assert.match(laidOutXml, /BPMNEdge/);
assert.match(laidOutXml, /Task_1/);

console.log('BPMN auto-layout verification passed.');
