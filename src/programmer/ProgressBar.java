/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package programmer;

import javax.swing.JProgressBar;

/**
 *
 * @author Федоренко Александр
 */
public class ProgressBar {
    
    JProgressBar progressBar;
    int maxValue = 100;
    
    public ProgressBar (JProgressBar jPB){
        progressBar = jPB;
    }
    
    public void setMax (int maxVal) {
        maxValue = maxVal;
    }
    
    public void setValue (int val) {
        int value;
        value = val*100/maxValue;
        progressBar.setValue(value);
    }
    
}
