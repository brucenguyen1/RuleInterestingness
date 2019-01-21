package com.rapidminer;

import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;

public class TestExpression {
  public static void main(String[] args) throws Exception{
    ScriptEngineManager mgr = new ScriptEngineManager();
    ScriptEngine engine = mgr.getEngineByName("JavaScript");
    String foo = "34.5 <= 35.1";
    System.out.println(engine.eval(foo));
    } 
}