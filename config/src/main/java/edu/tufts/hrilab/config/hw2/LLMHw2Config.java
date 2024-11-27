/*
 * Copyright © Thinking Robots, Inc., Tufts University, and others 2024.
 */

 package edu.tufts.hrilab.config.hw2;
 

 import edu.tufts.hrilab.action.GoalManagerComponent;
 import edu.tufts.hrilab.diarc.DiarcConfiguration;
 import edu.tufts.hrilab.nao.MockNaoComponent;
 import edu.tufts.hrilab.nao.NaoComponent;
 import edu.tufts.hrilab.simspeech.SimSpeechRecognitionComponent;
 import edu.tufts.hrilab.simspeech.SimSpeechProductionComponent;
 import edu.tufts.hrilab.slug.nlg.SimpleNLGComponent;
 import edu.tufts.hrilab.slug.parsing.llm.Hw2LLMParserComponent;
 import edu.tufts.hrilab.slug.parsing.tldl.TLDLParserComponent;
 import edu.tufts.hrilab.slug.pragmatics.PragmaticsComponent;
 import edu.tufts.hrilab.slug.refResolution.ReferenceResolutionComponent;
 import edu.tufts.hrilab.supermarket.SupermarketComponent;
 import edu.tufts.hrilab.asr.sphinx4.Sphinx4Component;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 public class LLMHw2Config extends DiarcConfiguration {
   // for logging
   protected static Logger log = LoggerFactory.getLogger(LLMHw2Config.class);
 
   /**
    * Set to true to use gui for speech input
    */
   public boolean simSpeech = true;
   /**
    * Set
    */
   public boolean mockNao = true;
   public boolean useSphinx = false;
 
   // start the configuration
   @Override
   public void runConfiguration() {

    if (simSpeech) {
      createInstance(SimSpeechRecognitionComponent.class,
              "-config speechinput.simspeech -speaker amitis -addressee roboshopper");
      createInstance(SimSpeechProductionComponent.class);
    }

    createInstance(edu.tufts.hrilab.llm.LLMComponent.class, "-endpoint http://vm-llama.eecs.tufts.edu:8080");

    createInstance(Hw2LLMParserComponent.class , "-service parseIt");
 
    createInstance(edu.tufts.hrilab.slug.listen.ListenerComponent.class);
 
    //createInstance(TLDLParserComponent.class, "-dict templatedict.dict templatedictLearned.dict");
 
    createInstance(PragmaticsComponent.class, "-pragrules demos.prag");
 
    createInstance(ReferenceResolutionComponent.class);
 
    createInstance(edu.tufts.hrilab.slug.dialogue.DialogueComponent.class);
 
    createInstance(SimpleNLGComponent.class);

    String gmArgs = "-beliefinitfile demos.pl domains/supermarket.pl agents/hw2agents.pl " + "-selector edu.tufts.hrilab.action.selector.GoalPlanningActionSelector " +
     "-asl core.asl vision.asl nao/naodemo.asl dialogue/nlg.asl dialogue/handleSemantics.asl dialogue/nlu.asl domains/supermarketRefactor.asl " +
     "-goal listen(self)";

    createInstance(GoalManagerComponent.class, gmArgs);

    createInstance(SupermarketComponent.class, "-groups agent:roboshopper -agentName roboshopper");
    

   }
 
 }
 
