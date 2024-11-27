package edu.tufts.hrilab.config.project;
import ai.thinkingrobots.trade.TRADE;
import ai.thinkingrobots.trade.TRADEException;
import edu.tufts.hrilab.action.GoalManagerComponent;
import edu.tufts.hrilab.action.planner.Planner; 
import edu.tufts.hrilab.diarc.DiarcConfiguration;
import edu.tufts.hrilab.simspeech.SimSpeechRecognitionComponent;
import edu.tufts.hrilab.simspeech.SimSpeechProductionComponent;
import edu.tufts.hrilab.slug.nlg.SimpleNLGComponent;
import edu.tufts.hrilab.slug.parsing.tldl.TLDLParserComponent;
import edu.tufts.hrilab.slug.pragmatics.PragmaticsComponent;
import edu.tufts.hrilab.slug.refResolution.ReferenceResolutionComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LLMplannerConfig extends DiarcConfiguration {

    // for logging
    protected static Logger log = LoggerFactory.getLogger(LLMplannerConfig.class);

    public boolean simSpeech = true;

    // start the configuration
    @Override
    public void runConfiguration() {

        if (simSpeech) {
            createInstance(SimSpeechRecognitionComponent.class,
                    "-config speechinput.simspeech -speaker amitis -addressee boxdropper");
            createInstance(SimSpeechProductionComponent.class);
        }

        // Create an instance of the LLM component for TRADE integration
        createInstance(edu.tufts.hrilab.llm.LLMComponent.class, "-endpoint http://vm-llama.eecs.tufts.edu:8080");

        // Create other components required for dialogue and parsing
        createInstance(edu.tufts.hrilab.slug.listen.ListenerComponent.class);
        createInstance(TLDLParserComponent.class, "-dict templatedict.dict templatedictLearned.dict");
        createInstance(PragmaticsComponent.class, "-pragrules demos.prag");
        createInstance(ReferenceResolutionComponent.class);
        createInstance(edu.tufts.hrilab.slug.dialogue.DialogueComponent.class);
        createInstance(SimpleNLGComponent.class);

        // Update GoalManager to use the Planner
        String gmArgs = "-beliefinitfile demos.pl domains/supermarket.pl agents/hw2agents.pl " +
                "-asl core.asl vision.asl nao/naodemo.asl dialogue/nlg.asl dialogue/handleSemantics.asl dialogue/nlu.asl domains/supermarketRefactor.asl " +
                "-goal listen(self) -planner edu.tufts.hrilab.action.planner.Planner";
        createInstance(GoalManagerComponent.class, gmArgs);

        log.info("LLMplannerConfig successfully loaded with Planner integration.");
    }
}
