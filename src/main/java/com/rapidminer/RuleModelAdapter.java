package com.rapidminer;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.learner.rules.RuleModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;

public class RuleModelAdapter extends Operator {
	private InputPort ruleModelInput = getInputPorts().createPort("rule text");	
	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private OutputPort ruleModelOutput = getOutputPorts().createPort("rule model");
	private OutputPort statsDataOutput = getOutputPorts().createPort("stats data");
	
	public RuleModelAdapter(OperatorDescription description) {
		super(description);
		
		ruleModelInput.addPrecondition(new SimplePrecondition(ruleModelInput, new MetaData(RuleModel.class)));		
		exampleSetInput.addPrecondition(new SimplePrecondition(exampleSetInput, new MetaData(ExampleSet.class)));
		
		getTransformer().addPassThroughRule(ruleModelInput, ruleModelOutput);
	}
}