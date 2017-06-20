/*
 *    HyperplaneGenerator.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 *    @author Albert Bifet (abifet at cs dot waikato dot ac dot nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package moa.streams.generators;

import java.util.Random; 

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.core.Example;
import moa.core.InstanceExample;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.streams.InstanceStream;
import moa.tasks.TaskMonitor;
import weka.core.FastVector;

/**
 * Stream generator for Hyperplane data stream.
 *
 * @author Albert Bifet (abifet at cs dot waikato dot ac dot nz),
 * @author Ammar Shaker (shaker at Mathematik dot Uni dash Marburg dot de)
 * @version $Revision: 7 $
 */
public class HyperplaneGeneratorReg extends AbstractOptionHandler implements
        InstanceStream {

    @Override
    public String getPurposeString() {
        return "Generates a problem of predicting class of a rotating hyperplane.";
    }

    private static final long serialVersionUID = 1L;

    public IntOption instanceRandomSeedOption = new IntOption(
            "instanceRandomSeed", 'i',
            "Seed for random generation of instances.", 1);

    public IntOption numAttsOption = new IntOption("numAtts", 'a',
            "The number of attributes to generate.", 10, 0, Integer.MAX_VALUE);

    public IntOption numDriftAttsOption = new IntOption("numDriftAtts", 'k',
            "The number of attributes with drift.", 2, 0, Integer.MAX_VALUE);

    public FloatOption magChangeOption = new FloatOption("magChange", 't',
            "Magnitude of the change for every example", 0.0, 0.0, 1.0);

    public IntOption noisePercentageOption = new IntOption("noisePercentage",
            'n', "Percentage of noise to add to the data.", 0, 0, 100);

    public IntOption sigmaPercentageOption = new IntOption("sigmaPercentage",
            's', "Percentage of probability that the direction of change is reversed.", 10, 0, 100);

	public MultiChoiceOption targetValueOption = new MultiChoiceOption(
            "targetValue", 'm', "The expected target value.", new String[]{
            		"Binary", "Distance",
                    "SquareDistance",
                    "CubicDistance"} ,
                new String[]{
        		 "Binary Classification",                     
                 "Distance To Hyperplane",
                 "square distance To Hyperplane",
                 "cubic distance To Hyperplane"
                }, 0);
	
    protected InstancesHeader streamHeader;

    protected Random instanceRandom;

    protected double[] weights;

    protected int[] sigma;

    public int numberInstance;

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
        monitor.setCurrentActivity("Preparing hyperplane...", -1.0);
        generateHeader();
        restart();
    }

    protected void generateHeader() {
        FastVector attributes = new FastVector();
        for (int i = 0; i < this.numAttsOption.getValue(); i++) {
            attributes.addElement(new Attribute("att" + (i + 1)));
        }

        FastVector classLabels = new FastVector();
        
        if (targetValueOption.getChosenIndex()==0 )
        {
        	for (int i = 0; i < 2 ; i++) {
                classLabels.addElement("class" + (i + 1));
            }
        	attributes.addElement(new Attribute("class", classLabels));
        }
        else
        {
        	attributes.addElement(new Attribute("TargetValue"));
        }        

        this.streamHeader = new InstancesHeader(new Instances(
                getCLICreationString(InstanceStream.class), attributes, 0));
        this.streamHeader.setClassIndex(this.streamHeader.numAttributes() - 1);
    }

    @Override
    public long estimatedRemainingInstances() {
        return -1;
    }

    @Override
    public InstancesHeader getHeader() {
        return this.streamHeader;
    }

    @Override
    public boolean hasMoreInstances() {
        return true;
    }

    @Override
    public boolean isRestartable() {
        return true;
    }

    @Override
    public Example<Instance> nextInstance() {

        int numAtts = this.numAttsOption.getValue();
        double[] attVals = new double[numAtts + 1];
        double sum = 0.0;
        double sumWeights = 0.0;
        double sumSquareWeights=0.0 ;
        
        for (int i = 0; i < numAtts; i++) {
            attVals[i] = this.instanceRandom.nextDouble();
            sum += this.weights[i] * attVals[i];
            sumWeights += this.weights[i];
			sumSquareWeights += Math.pow(this.weights[i],2) ;
        }
        double classLabel=0 ;
        if (targetValueOption.getChosenIndex()==0)
			if (sum >= sumWeights * 0.5) {
	            classLabel = 1;
	        } else {
	            classLabel = 0;
	        }
		else if (targetValueOption.getChosenIndex()==1)
			classLabel = sum / Math.sqrt(sumSquareWeights) ;
        else if (targetValueOption.getChosenIndex()==2)
        	classLabel = Math.pow(sum,2) /sumSquareWeights ; 
        else if (targetValueOption.getChosenIndex()==3)
        	classLabel = Math.pow(sum / Math.sqrt(sumSquareWeights),3) ;
        
        //Add Noise
        if ((1 + (this.instanceRandom.nextInt(100))) <= this.noisePercentageOption.getValue()) {
        	
        	if (targetValueOption.getChosenIndex()==0)
        	{
        		if ((1 + (this.instanceRandom.nextInt(100))) <= this.noisePercentageOption.getValue()) {
        			classLabel = (classLabel == 0 ? 1 : 0);
        		}
        	}        
        	else
        	{
	       		// the range of noise added to the distance increases by the increase of the number of dimentsions  
	       		double temp= this.instanceRandom.nextInt(1000) ;
	       		temp = temp * Math.sqrt(numAtts)/1000 ;
	       		
	       		if (targetValueOption.getChosenIndex()==1)
	       			temp = Math.pow(temp, 1) ;
	            else if (targetValueOption.getChosenIndex()==2)
	            	temp = Math.pow(temp, 2) ;
	            else if (targetValueOption.getChosenIndex()==3)
	            	temp = Math.pow(temp, 3) ;
	       		
	       		if (this.instanceRandom.nextInt(2)==1)
	       			classLabel-=temp ;
	       		else
	       			classLabel+=temp ;
	       		classLabel=Math.abs(classLabel) ;
        	}
        }
        
        //Instance inst = new DenseInstance(1.0, attVals);
        Instance inst = new DenseInstance(1.0, attVals);
        inst.setDataset(getHeader());
        inst.setClassValue(classLabel);
        addDrift();
        
        return new InstanceExample(inst);
    }

    private void addDrift() {
        for (int i = 0; i < this.numDriftAttsOption.getValue(); i++) {
            this.weights[i] += (double) ((double) sigma[i]) * ((double) this.magChangeOption.getValue());
            if (//this.weights[i] >= 1.0 || this.weights[i] <= 0.0 ||
                    (1 + (this.instanceRandom.nextInt(100))) <= this.sigmaPercentageOption.getValue()) {
                this.sigma[i] *= -1;
            }
        }
    }

    @Override
    public void restart() {
        this.instanceRandom = new Random(this.instanceRandomSeedOption.getValue());
        this.weights = new double[this.numAttsOption.getValue()];
        this.sigma = new int[this.numAttsOption.getValue()];
        for (int i = 0; i < this.numAttsOption.getValue(); i++) {
            this.weights[i] = this.instanceRandom.nextDouble();
            this.sigma[i] = (i < this.numDriftAttsOption.getValue() ? 1 : 0);
        }
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }
}