/**
 * 
 */
package com.rapidminer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.rules.Rule;
import com.rapidminer.operator.learner.rules.RuleModel;
import com.rapidminer.operator.learner.tree.SplitCondition;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.tools.Ontology;

/** 
 * Compute interestingness measures for rules. RuleModel contains all
 * rules as input. Output is an ExampleSet including every rule and 
 * the interestingness measures computed. This ExampleSet can be exported
 * to CSV via the Write to CSV operator.
 * @author Bruce
 *
 */
public class RuleInterestingnessOperator extends Operator {
	public static double MAX_VALUE = 99999;
	public static String MEASURE_COVERAGE = "Coverage";
	public static String MEASURE_SUPPORT = "Support";
	
//	public static String MEASURE_CONFIDENCE_PRECISION = "confidence_precision";
//	public static String MEASURE_RECALL = "recall";
//	public static String MEASURE_PREVALENCE = "prevalence";
//	public static String MEASURE_SPECIFICITY = "specificity";
//	public static String MEASURE_ACCURACY = "accuracy";
//	public static String MEASURE_LIFT_INTEREST = "lift_interest";
//	public static String MEASURE_LEVERAGE = "leverage";
//	public static String MEASURE_ADDED_VALUE = "added_value";
//	public static String MEASURE_RELATIVE_RISK = "relative_risk";
//	public static String MEASURE_JACCARD = "jaccard";
//	public static String MEASURE_CERTAINTY_FACTOR = "certainty_factor";
	
	public static String MEASURE_ODDS_RATIO = "Odds ratio";
	public static String MEASURE_YULESQ = "Yule's Q";
	public static String MEASURE_YULESY = "Yule's Y";
	public static String MEASURE_COLLECTIVE_STRENGTH = "Collective Strength";
	public static String MEASURE_TWO_WAY_SUPP = "Two-way Support";
	public static String MEASURE_PHI_COEFFICIENT = "Phi Coefficient";
	public static String MEASURE_PIATETSKY_SHAPIRO = "Piatetsky Shapiro";
	public static String MEASURE_INFO_GAIN = "Information Gain";
	
	public static String PARAM_RULE_CONFIDENCE = "Minimum rule confidence (from rule model)";
	public static String PARAM_RULE_COVERAGE = "Minimum rule coverage (from rule model)";

	private InputPort ruleModelInput = getInputPorts().createPort("rule model");	
	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private OutputPort ruleModelOutput = getOutputPorts().createPort("rule model");
	private OutputPort statsDataOutput = getOutputPorts().createPort("stats data");
	
	private ScriptEngineManager mgr = new ScriptEngineManager();
    private ScriptEngine engine = mgr.getEngineByName("JavaScript");
	
	public RuleInterestingnessOperator(OperatorDescription description) {
		super(description);
		
		ruleModelInput.addPrecondition(new SimplePrecondition(ruleModelInput, new MetaData(RuleModel.class)));		
		exampleSetInput.addPrecondition(new SimplePrecondition(exampleSetInput, new MetaData(ExampleSet.class)));
		
		getTransformer().addPassThroughRule(ruleModelInput, ruleModelOutput);
	}
	
//	@Override
//	public List<ParameterType> getParameterTypes() {
//		List<ParameterType> types = super.getParameterTypes();
//		types.add(new ParameterTypeDouble(MEASURE_COVERAGE, MEASURE_COVERAGE, 0, 1, 0.00, false));
//		types.add(new ParameterTypeDouble(MEASURE_SUPPORT, MEASURE_SUPPORT, 0, 1, 0.00, false));
//		types.add(new ParameterTypeDouble(MEASURE_CONFIDENCE_PRECISION, MEASURE_CONFIDENCE_PRECISION, 0, 1, 0.00, false));
//		types.add(new ParameterTypeDouble(MEASURE_RECALL, MEASURE_RECALL, 0, 1, 0.00, false));
//		types.add(new ParameterTypeDouble(MEASURE_PREVALENCE, MEASURE_PREVALENCE, 0, 1, 0.00, false));
//		types.add(new ParameterTypeDouble(MEASURE_SPECIFICITY, MEASURE_SPECIFICITY, 0, 1, 0.00, false));
//		types.add(new ParameterTypeDouble(MEASURE_ACCURACY, MEASURE_ACCURACY, 0, 1, 0.00, false));
//		types.add(new ParameterTypeDouble(PARAM_RULE_COVERAGE, PARAM_RULE_COVERAGE, 0, 1, 0.01, false));
//		types.add(new ParameterTypeDouble(PARAM_RULE_CONFIDENCE, PARAM_RULE_CONFIDENCE, 0, 1, 0.8, false));
//		return types;
//	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void doWork() throws OperatorException {
		RuleModel ruleModel = ruleModelInput.getData();
		ExampleSet inputExampleSet = exampleSetInput.getData();
		// stats contains basic statistical measures.
		// rule: A => B
		// stats[i][0] is P(A) of rule i
		// stats[i][1] is P(B) of rule i
		// stats[i][2] is P(A and B)
		// stats[i][3] is P(A and -B)
		// stats[i][4] is P(-A and B)
		double[][] stats = new double[ruleModel.getRules().size()][6]; 
		Map<String, double[]> measuresMap = new HashMap();
		
		measuresMap.put(MEASURE_COVERAGE, new double[ruleModel.getRules().size()]);
		measuresMap.put(MEASURE_SUPPORT, new double[ruleModel.getRules().size()]);

//		measuresMap.put(MEASURE_CONFIDENCE_PRECISION, new double[ruleModel.getRules().size()]);
//		measuresMap.put(MEASURE_RECALL, new double[ruleModel.getRules().size()]);
//		measuresMap.put(MEASURE_PREVALENCE, new double[ruleModel.getRules().size()]);
//		measuresMap.put(MEASURE_SPECIFICITY, new double[ruleModel.getRules().size()]);
//		measuresMap.put(MEASURE_ACCURACY, new double[ruleModel.getRules().size()]);
//		measuresMap.put(MEASURE_LIFT_INTEREST, new double[ruleModel.getRules().size()]);
//		measuresMap.put(MEASURE_LEVERAGE, new double[ruleModel.getRules().size()]);
//		measuresMap.put(MEASURE_ADDED_VALUE, new double[ruleModel.getRules().size()]);
//		measuresMap.put(MEASURE_RELATIVE_RISK, new double[ruleModel.getRules().size()]);
//		measuresMap.put(MEASURE_JACCARD, new double[ruleModel.getRules().size()]);
//		measuresMap.put(MEASURE_CERTAINTY_FACTOR, new double[ruleModel.getRules().size()]);	
		
		measuresMap.put(MEASURE_ODDS_RATIO, new double[ruleModel.getRules().size()]);
		measuresMap.put(MEASURE_YULESQ, new double[ruleModel.getRules().size()]);
		measuresMap.put(MEASURE_YULESY, new double[ruleModel.getRules().size()]);
		measuresMap.put(MEASURE_COLLECTIVE_STRENGTH, new double[ruleModel.getRules().size()]);
		measuresMap.put(MEASURE_TWO_WAY_SUPP, new double[ruleModel.getRules().size()]);
		measuresMap.put(MEASURE_PHI_COEFFICIENT, new double[ruleModel.getRules().size()]);
		measuresMap.put(MEASURE_PIATETSKY_SHAPIRO, new double[ruleModel.getRules().size()]);
		measuresMap.put(MEASURE_INFO_GAIN, new double[ruleModel.getRules().size()]);			
		
		// Calculate basic statistics
		System.out.println("CREATING BASIC STATISTICAL MEASURES...");
		try {
			Attribute label = inputExampleSet.getAttributes().getLabel();
			for (int i = 0; i < ruleModel.getRules().size(); i++) {
				Rule rule = ruleModel.getRules().get(i);
				
//				List<String> labelValues = inputExampleSet.getAttributes().getLabel().getMapping().getValues();
//				int labelConfIndex = 0;
//				double ruleConfidence = 0;
//				double ruleSupport = 0;
//				for (int k=0;k < labelValues.size(); k++) { 
//					if (labelValues.get(k).equals(rule.getLabel()))  {
//						labelConfIndex = k;
//						ruleConfidence = rule.getConfidences()[labelConfIndex];
//						ruleSupport = (1.0*rule.getFrequencies()[labelConfIndex] / inputExampleSet.size()); 
//						break;
//					}
//				}
				
				//Calculate basic stats for rule i
				System.out.println("Computing for Rule " + i + "/" + (ruleModel.getRules().size()) + "...");
				for (int j = 0; j<inputExampleSet.size(); j++) {
					Example example = inputExampleSet.getExample(j);
					Boolean A = this.coversAllExampleConditions(rule, example);
					Boolean B = this.coversExampleLabel(rule, example);
					if (A) {
						stats[i][0]++; // P(A)
					}
					if (B) {
						stats[i][1]++; //P(B)
					}
					if (A && B) {
						stats[i][2]++; // P(A and B)
					}
					if (A && !B) {
						stats[i][3]++; // P(A and -B)
					}
					if (!A && B) {
						stats[i][4]++; // P(-A and B)
					}
					if (!A && !B) {
						stats[i][5]++; // P(-A and -B)
					}					
				}	
					
				// stats[i][0] is P(A) of rule i
				// stats[i][1] is P(B) of rule i
				// stats[i][2] is P(A and B)
				// stats[i][3] is P(A and -B)
				// stats[i][4] is P(-A and B)
				// stats[i][5] is P(-A and -B
				stats[i][0] = 1.0*stats[i][0]/inputExampleSet.size();
				stats[i][1] = 1.0*stats[i][1]/inputExampleSet.size();
				stats[i][2] = 1.0*stats[i][2]/inputExampleSet.size();
				stats[i][3] = 1.0*stats[i][3]/inputExampleSet.size();
				stats[i][4] = 1.0*stats[i][4]/inputExampleSet.size();
				stats[i][5] = 1.0*stats[i][5]/inputExampleSet.size();
				
				//Calculate interestingness measures for rule i
//				if (stats[i][0] > 0) {
//					measuresMap.get(MEASURE_CONFIDENCE_PRECISION)[i] += (stats[i][2]/stats[i][0]);
//				} 
//				if (stats[i][1] > 0) {
//					measuresMap.get(MEASURE_RECALL)[i] += (stats[i][2]/stats[i][1]);
//				} 
//				measuresMap.get(MEASURE_PREVALENCE)[i] += stats[i][1];
//				measuresMap.get(MEASURE_SPECIFICITY)[i] += (1 - stats[i][0] - stats[i][1] + stats[i][2])/(1 - stats[i][0]);
//				measuresMap.get(MEASURE_ACCURACY)[i] += (stats[i][2] + (1 - stats[i][0] - stats[i][1] + stats[i][2])); //Wrong
//				measuresMap.get(MEASURE_LIFT_INTEREST)[i] += (stats[i][2]/(stats[i][0]*stats[i][1]));
//				measuresMap.get(MEASURE_LEVERAGE)[i] += (stats[i][2]/stats[i][0] - stats[i][0]*stats[i][1]);
//				measuresMap.get(MEASURE_ADDED_VALUE)[i] += (stats[i][2]/stats[i][0] - stats[i][1]);
//				measuresMap.get(MEASURE_RELATIVE_RISK)[i] += (stats[i][2]/stats[i][0])*((1-stats[i][0])/stats[i][4]);
//				measuresMap.get(MEASURE_JACCARD)[i] += stats[i][2]/(stats[i][0] + stats[i][1] - stats[i][2]);
//				measuresMap.get(MEASURE_CERTAINTY_FACTOR)[i] += (stats[i][2]/stats[i][0] - stats[i][1])/(1 - stats[i][1]);
				
				measuresMap.get(MEASURE_COVERAGE)[i] = stats[i][0];
				
				measuresMap.get(MEASURE_SUPPORT)[i] = stats[i][2];
				
				if (stats[i][3]*stats[i][4] != 0) {
					measuresMap.get(MEASURE_ODDS_RATIO)[i] = stats[i][2]*stats[i][5]/(stats[i][3]*stats[i][4]);
				}
				else {
					measuresMap.get(MEASURE_ODDS_RATIO)[i] = this.MAX_VALUE;
				}
				
				double YULESQ_1 = 1.0*stats[i][2]*stats[i][5] - 1.0*stats[i][3]*stats[i][4];
				double YULESQ_2 = 1.0*stats[i][2]*stats[i][5] + 1.0*stats[i][3]*stats[i][4];
				if (YULESQ_2 != 0) {
					measuresMap.get(MEASURE_YULESQ)[i] = 1.0*YULESQ_1/YULESQ_2;
				}
				else {
					measuresMap.get(MEASURE_YULESQ)[i] = this.MAX_VALUE;
				}
				
				double YULESY_1 = 1.0*Math.sqrt(stats[i][2]*stats[i][5]) - 1.0*Math.sqrt(stats[i][3]*stats[i][4]);
				double YULESY_2 = 1.0*Math.sqrt(stats[i][2]*stats[i][5]) + 1.0*Math.sqrt(stats[i][3]*stats[i][4]);
				if (YULESY_2 != 0) {
					measuresMap.get(MEASURE_YULESY)[i] = 1.0*YULESY_1/YULESY_2;
				}
				else {
					measuresMap.get(MEASURE_YULESY)[i] = this.MAX_VALUE;
				}
				
				//double COLLECTIVE_STRENGTH_11 = stats[i][2] + stats[i][5]/(1-stats[i][0]);
				//double COLLECTIVE_STRENGTH_12 = stats[i][0]*stats[i][1] + (1-stats[i][0])*(1-stats[i][1]);
				double COLLECTIVE_STRENGTH_11 = stats[i][2] + stats[i][5];
				double COLLECTIVE_STRENGTH_12 = stats[i][0]*stats[i][1] + (1-stats[i][0])*(1-stats[i][1]);
				double COLLECTIVE_STRENGTH_21 = 1 - COLLECTIVE_STRENGTH_12;
				double COLLECTIVE_STRENGTH_22 = 1 - COLLECTIVE_STRENGTH_11;
				if (COLLECTIVE_STRENGTH_12 != 0 && COLLECTIVE_STRENGTH_22 != 0) {
					measuresMap.get(MEASURE_COLLECTIVE_STRENGTH)[i] = (COLLECTIVE_STRENGTH_11/COLLECTIVE_STRENGTH_12)*(COLLECTIVE_STRENGTH_21/COLLECTIVE_STRENGTH_22);
				}
				else {
					measuresMap.get(MEASURE_COLLECTIVE_STRENGTH)[i] = this.MAX_VALUE;
				}
				
				if (stats[i][0]*stats[i][1] != 0) {
					double lift = (stats[i][2]/(stats[i][0]*stats[i][1]));
					measuresMap.get(MEASURE_TWO_WAY_SUPP)[i] = stats[i][2]*Math.log(lift)/Math.log(2);
					measuresMap.get(MEASURE_INFO_GAIN)[i] = Math.log(lift);
				}
				else {
					measuresMap.get(MEASURE_TWO_WAY_SUPP)[i] = this.MAX_VALUE;
					measuresMap.get(MEASURE_INFO_GAIN)[i] = this.MAX_VALUE;
				}
				
				measuresMap.get(MEASURE_PIATETSKY_SHAPIRO)[i] = stats[i][2] - stats[i][0]*stats[i][1];
				
				if (Math.sqrt(stats[i][0]*stats[i][1]*(1-stats[i][0])*(1-stats[i][1])) != 0) {
					measuresMap.get(MEASURE_PHI_COEFFICIENT)[i] = (stats[i][2] - stats[i][0]*stats[i][1])/Math.sqrt(stats[i][0]*stats[i][1]*(1-stats[i][0])*(1-stats[i][1]));
				}
				else {
					measuresMap.get(MEASURE_PHI_COEFFICIENT)[i] = this.MAX_VALUE;
				}
					
				
			}
		}
		catch (ScriptException ex) {
			throw new OperatorException(ex.getMessage());
		}
		
		//Write output to an ExampleSet
		ExampleSet outputExampleSet = this.createExampleSetOutput(measuresMap);
		statsDataOutput.deliver(outputExampleSet);
	}
	
	/**
	 * Create an example set including these columns: 
	 * 	- Rule content (e.g. if attribute_11 > 0.171 and attribute_45 > 0.264 then Mine  (0 / 34))
	 * 	- One column for each rule interestingness measure
	 * Note: the 0 / 34 above indicates frequency of each class label, here Mine is 34 and Rock is 0.
	 * measureMap: key - name of measure, value - array containing measure value for each rule
	 * @return ExampleSet
	 * @throws OperatorException 
	 */
	private ExampleSet createExampleSetOutput(Map<String, double[]> measuresMap) throws OperatorException {
		final int NUMBEROFMEASURES = measuresMap.size();
		 
		RuleModel ruleModel = ruleModelInput.getData();
		int numberOfLabelValues = ruleModel.getLabel().getMapping().size(); 
		
		// create attribute list
		System.out.println("CREATING ATTRIBUTE LIST...");
		Attribute[] attributes = new Attribute[3 + NUMBEROFMEASURES + 2*numberOfLabelValues]; 
		attributes[0] = AttributeFactory.createAttribute("Rule", Ontology.STRING); //rule string
		attributes[1] = AttributeFactory.createAttribute("Label", Ontology.STRING); //label column
		attributes[2] = AttributeFactory.createAttribute("Length", Ontology.STRING); //label column
		
		//System.out.println("Creating interestingness measure columns...");
		int measureIndex = 0;
		for (String measureName : measuresMap.keySet()) { // interestingness measure
			attributes[measureIndex+3] = AttributeFactory.createAttribute (measureName, Ontology.REAL);
			measureIndex++;
		}
		
		//System.out.println("Creating label frequency columns...");
		int attrIndex = NUMBEROFMEASURES + 3;
		for (String labelValue : ruleModel.getLabel().getMapping().getValues()) { // each label frequency
			attributes[attrIndex] = AttributeFactory.createAttribute (labelValue + "_Freq", Ontology.INTEGER);
			attrIndex++;		
		}	
		
		//System.out.println("Creating label confidence columns...");
		for (String labelValue : ruleModel.getLabel().getMapping().getValues()) { // each label confidence
			attributes[attrIndex] = AttributeFactory.createAttribute (labelValue + "_Conf", Ontology.REAL);
			attrIndex++;			
		}		
		
		//create table from attribute list (columns only)
		//System.out.println("CREATING EXAMPLE TABLE...");
		MemoryExampleTable table = new MemoryExampleTable(attributes);
		DataRowFactory ROW_FACTORY = new DataRowFactory(0, '.');
		
		//add data to table
		System.out.println("ADDING DATA TO THE TABLE FOR EVERY RULE...");
		for (int i = 0; i < measuresMap.get(MEASURE_COVERAGE).length; i++) {
//			if (measuresMap.get(MEASURE_COVERAGE)[i] >= getParameterAsDouble(MEASURE_COVERAGE) &&
//				measuresMap.get(MEASURE_SUPPORT)[i] >= getParameterAsDouble(MEASURE_SUPPORT) &&
//				measuresMap.get(MEASURE_CONFIDENCE_PRECISION)[i] >= getParameterAsDouble(MEASURE_CONFIDENCE_PRECISION) &&
//				measuresMap.get(MEASURE_RECALL)[i] >= getParameterAsDouble(MEASURE_RECALL) &&
//				measuresMap.get(MEASURE_PREVALENCE)[i] >= getParameterAsDouble(MEASURE_PREVALENCE) &&
//				measuresMap.get(MEASURE_SPECIFICITY)[i] >= getParameterAsDouble(MEASURE_SPECIFICITY) &&
//				measuresMap.get(MEASURE_ACCURACY)[i] >= getParameterAsDouble(MEASURE_ACCURACY)) {
			
				String[] dataRow = new String[attributes.length];
				dataRow[0] = ruleModel.getRules().get(i).toString(); //rule string (accordingly to the header above
				dataRow[1] = ruleModel.getRules().get(i).getLabel(); // rule label
				dataRow[2] = Integer.toString(ruleModel.getRules().get(i).getTerms().size()); // rule length (number of premise terms)
				
				//System.out.println("Adding interestingness measures...");
				measureIndex = 0;
				for (String measureName : measuresMap.keySet()) { // interestingness measure
					dataRow[measureIndex+3] = String.valueOf(measuresMap.get(measureName)[i]);
					measureIndex++;
				}
				
				//System.out.println("Adding label frequency columns...");
				int[] freqs = ruleModel.getRules().get(i).getFrequencies(); // each label frequency value
				for (int freqIndex = 0; freqIndex < freqs.length; freqIndex++) {
					dataRow[freqIndex+3+NUMBEROFMEASURES] = String.valueOf(freqs[freqIndex]);
				}
				
				//System.out.println("Adding label confidence columns...");
				//Note that the confidence[i] is the confidence value of the label [i]
				//in label.getMapping().getValues()).
				double[] confs = ruleModel.getRules().get(i).getConfidences(); // each label confidence value
				for (int confIndex = 0; confIndex < confs.length; confIndex++) {
					dataRow[confIndex+3+NUMBEROFMEASURES+freqs.length] = String.valueOf(confs[confIndex]);
				}			
				
				DataRow tableRow = ROW_FACTORY.create(dataRow, attributes);
				table.addDataRow(tableRow);				
//			}
			
		}
		
		// create example set
		ExampleSet exampleSet = table.createExampleSet();	
		
		return exampleSet;
	}
	
	private Boolean coversExampleCondition(SplitCondition condition, Example example) throws ScriptException {
		String exampleValue = example.getValueAsString(example.getAttributes().get(condition.getAttributeName()));
		String conditionRelation = condition.getRelation();
		if (conditionRelation.contains("\u2265")) {
			conditionRelation = ">=";
		}
		else if (conditionRelation.contains("\u2264")) {
			conditionRelation = "<=";
		}
		String conditionValue = condition.getValueString();
		String expression = exampleValue + " " + conditionRelation + " " + conditionValue; 
		//System.out.println("Evaluating: " + expression);
		//System.out.println("Condition: " + condition.toString());
		
	    Boolean result = (Boolean)(engine.eval(expression));	
		
		return result;
	}
	
	private Boolean coversAllExampleConditions(Rule rule, Example example) throws ScriptException {
		for (SplitCondition term : rule.getTerms()) {
			if (!coversExampleCondition(term,example)) {
				return false;
			}
		}
		return true;
		//return rule.coversExample(example);
	}
	
	private Boolean coversExampleLabel(Rule rule, Example example) throws ScriptException {
		String exampleValue = example.getValueAsString(example.getAttributes().getLabel());
		String ruleLabelValue = rule.getLabel();	
		return (exampleValue == ruleLabelValue);
	}
}
