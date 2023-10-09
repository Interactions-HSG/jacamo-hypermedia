package signifiers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import jason.asSemantics.Agent;
import jason.asSemantics.Option;
import jason.asSemantics.Unifier;
import jason.asSyntax.Literal;
import jason.asSyntax.Plan;
import jason.asSyntax.PlanBody;
import jason.asSyntax.PredicateIndicator;
import jason.asSyntax.PlanBody.BodyType;
import jason.asSyntax.Term;
import jason.asSyntax.StringTerm;
import jason.asSyntax.LogicalFormula;
import java.util.HashSet;
import java.util.Set;
import java.lang.String;

import static jason.asSyntax.ASSyntax.*;
import jason.asSyntax.parser.ParseException;


public class SignifierResolutionMechanism1 extends Agent {

    /* Get exposed actions from the beliefs
    Assuming the artifact exposes a belief like signifier(action, context, ability)[artifact_name(artifact)]

    signifier(turnOff, nobodyInTheRoom, manipulateLight)[artifact_name(light)].
    signifier(turnOn, true, manipulateLight)[artifact_name(light)].

    */
    public Set<Action> getExposedActions() {

        PredicateIndicator predicate_indicator = new PredicateIndicator("signifier", 3);

        Iterator<Literal> candidate_beliefs = getBB().getCandidateBeliefs(predicate_indicator);

        Set<Action> exposed_actions = new HashSet<Action>();

        if(candidate_beliefs == null){
            return exposed_actions;
        }

        while (candidate_beliefs.hasNext()){
            Literal candidate_belief = candidate_beliefs.next();

            Term action_name = candidate_belief.getTerm(0);
            Term action_resource = candidate_belief.getAnnot("artifact_name").getTerm(0);

            exposed_actions.add(new Action(action_name, action_resource));
        }

        return exposed_actions;
    }

    /* Get exposed context from the beliefs
    Assuming we have beliefs like ability(some_ability) */
    public Set<Ability> getAgentAbilities() {

        PredicateIndicator predicate_indicator = new PredicateIndicator("ability", 1);

        Iterator<Literal> candidate_beliefs = getBB().getCandidateBeliefs(predicate_indicator);

        Set<Ability> abilities = new HashSet<Ability>();

        if(candidate_beliefs == null){
            return abilities;
        }

        while (candidate_beliefs.hasNext()){
            Literal candidate_belief = candidate_beliefs.next();

            Term ability_name = candidate_belief.getTerm(0);

            abilities.add(new Ability(ability_name));
        }

        return abilities;
    }

    /* Getting actions from the plan body */
    public Set<Action> getPlanActions(Option option){

        Plan plan = option.getPlan();

        Set<Action> plan_actions_list = new HashSet<Action>();

        PlanBody plan_body = plan.getBody();

        while(plan_body != null){

            if(plan_body.getBodyType() == BodyType.action){

                Literal body_term = (Literal)plan_body.getBodyTerm();

                if(body_term.getFunctor().equals("invokeAction")){
                    Term action_resource = body_term.getAnnot("artifact_name").getTerm(0);

                    if(option.getUnifier().get(action_resource.toString()) != null){
                        action_resource = option.getUnifier().get(action_resource.toString());
                    }
                    Term action_name = body_term.getTerm(0);

                    plan_actions_list.add(new Action(action_name, action_resource));
                }

            }

            plan_body = plan_body.getBodyNext();

        }

        return plan_actions_list;
    }

    /* Overriding selectOption function */
    @Override
    public Option selectOption(List<Option> options) {

        if (options != null && !options.isEmpty()) {

            /* Creating a set of exposed actions, context and abilities */
            Set<Action> exposed_actions = getExposedActions();
            System.out.println("\nExposed actions: "+ exposed_actions);

            Set<Ability> agent_abilities = getAgentAbilities();
            System.out.println("Agent abilities: "+ agent_abilities);

            for (Option option: options) {

                /* Creating a set of plan actions and checking if it is a subset of exposed actions */
                Set<Action> plan_actions = getPlanActions(option);
                System.out.println("\nActions required for " + option.getPlan().getTrigger() +": " + plan_actions);
                if (!exposed_actions.containsAll(plan_actions)){
                    System.out.println("One or more actions from " + plan_actions +" are not available, skipping plan...");
                } else{
                    System.out.println(plan_actions);
                    return option;
                }
            }
        }

        return null;

    }

}