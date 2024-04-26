/*
 * Copyright © Thinking Robots, Inc., Tufts University, and others 2024.
 */

package edu.tufts.hrilab.action.db;

import ai.thinkingrobots.trade.TRADEService;
import ai.thinkingrobots.trade.TRADEServiceConstraints;
import edu.tufts.hrilab.action.ActionBinding;
import edu.tufts.hrilab.action.Effect;
import edu.tufts.hrilab.action.annotations.Action;
import edu.tufts.hrilab.action.db.util.Utilities;
import edu.tufts.hrilab.fol.Predicate;
import edu.tufts.hrilab.fol.Symbol;
import edu.tufts.hrilab.fol.Variable;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ActionDatabase {
  private final static Logger log = LoggerFactory.getLogger(ActionDatabase.class);

  /**
   * Primitive ActionDBEntries.
   */
  private final Set<ActionDBEntry> primitives = new HashSet<>();

  /**
   * Script (i.e., from ASL) ActionDBEntries.
   */
  private final Set<ActionDBEntry> scripts = new HashSet<>();

  /**
   * A map of all the actions keyed by their type (action name).
   */
  private final Map<String, List<ActionDBEntry>> actionDB = new HashMap<>();

  /**
   * A map of all actions keyed by their postconditions functor name (e.g., "holding" for post-cond holding(?actor,?obj)).
   */
  private final Map<String, List<Pair<Predicate, ActionDBEntry>>> postcondDB = new HashMap<>();

  /**
   * A map of all the disabled actions keyed by their type.
   */
  private final Map<String, List<ActionDBEntry>> disabledActionDB = new HashMap<>();

  /**
   * Protected so that only the Database can instantiate.
   */
  protected ActionDatabase() {
  }

  /**
   * Insert the action into the database. The action is keyed by its type. This
   * function is internally synchronized on the singleton.
   *
   * @param entry the database entry defining this action
   */
  @TRADEService
  protected final synchronized void putAction(ActionDBEntry entry) {

    // remove entry from disabledActionDB (if it exists)
    if (disabledActionDB.containsKey(entry.getType())) {
      disabledActionDB.get(entry.getType()).remove(entry);
    }

    // check action for Strings and print deprecation warning.
    // case 1: String in input or return arg
    List<ActionBinding> stringRoles = entry.getRoles().stream().filter(role -> !role.isLocal() && role.getJavaType().equals(String.class)).collect(Collectors.toList());
    if (!stringRoles.isEmpty()) {
      log.debug("DEPRECATED: Action uses java.lang.String for input/output arguments. Change arg(s) to FOL class(es). Action: " + entry);
    }
    // case 2: String used in Effect
    for (Effect effect : entry.getEffects()) {
      if (effect.isAutoGenerated()) {
        // don't need to check autogenerated, those will already get caught in the input/output arg check
        continue;
      }
      for (Variable var : effect.getPredicate().getVars()) {
        if (entry.getRole(var.getName()) != null && entry.getRole(var.getName()).getJavaType().equals(String.class)) {
          log.debug("DEPRECATED: Action contains Effect that uses java.lang.String. Change to FOL class. Action: " + entry);
        }
      }
    }

    log.trace("Adding new action to DB: " + entry);
    Utilities.putEntry(actionDB, entry);
    addPostcondsToDB(entry);
    if (entry.isPrimitive()) {
      primitives.add(entry);
    } else {
      scripts.add(entry);
    }
  }

  /**
   * Lookup action by type. Returns if a matching action exists.
   *
   * @param type the type of the action to look up
   * @return if a matching action exists
   */
  public final boolean actionExists(String type) {
    ActionDBEntry adb = Utilities.getEntry(actionDB, type);
    return adb != null;
  }

  /**
   * Lookup action by type. Returns last added entry if more than one action
   * with type.
   *
   * @param type the type of the action to look up
   * @return the requested action (last added), if found, null otherwise
   */
  @TRADEService
  public final ActionDBEntry getAction(String type) {
    ActionDBEntry adb = Utilities.getEntry(actionDB, type);
    if (adb == null) {
      log.warn("[getAction] Could not find action for type: " + type);
    }
    return adb;
  }

  //TODO:brad: this is only used in tests, are those tests valid? should we get rid of this?

  /**
   * Lookup action by type and roles.
   *
   * @param type  the type of the action to look up
   * @param roles the roles of the action to look up
   * @return the requested action, if found, null otherwise
   */
  @TRADEService
  public final ActionDBEntry getAction(String type, List<Class<?>> roles) {
    ActionDBEntry adb = Utilities.getEntry(actionDB, type, roles);
    if (adb == null) {
      log.warn("[getAction] Could not find action for type " + type + " with role types " + roles);
    }
    return adb;
  }

  /**
   * Lookup action by type, roles and agent name
   *
   * @param type           the type of the action to look up
   * @param actor          the actor affected by the action to look up
   * @param inputRoleTypes the java types of the input roles of the action to look up
   * @return the requested action, if found, null otherwise
   */
  @TRADEService
  public final ActionDBEntry getAction(String type, Symbol actor, List<Class<?>> inputRoleTypes) {
    ActionDBEntry adb = Utilities.getEntry(actionDB, type, actor, inputRoleTypes);
    if (adb == null) {
      log.warn("[getAction] Could not find action for type " + type + " with role types " + inputRoleTypes + " (for actor " + actor + ")");
    }
    return adb;
  }

  /**
   * Add an action's postconditions to the database. This should only be called
   * internally when an ActionDBEntry is added.
   *
   * @param action the action for which the postconditions should be added
   */
  private synchronized void addPostcondsToDB(ActionDBEntry action) {
    for (Effect post : action.getPostConditions()) {
      String name = post.getPredicate().getName();
      List<Pair<Predicate, ActionDBEntry>> map;
      Pair<Predicate, ActionDBEntry> pair = Pair.of(post.getPredicate(), action);

      log.debug("putting postcond into DB: " + post);

      map = postcondDB.get(name);

      if (map == null) {
        // this post condition hasn't been seen before, add it so this action can be looked up
        map = new ArrayList<>();
        map.add(pair);
        postcondDB.put(name, map);
      } else {
        // this post condition has been seen before, add DBEntry to the list if it is not already present
        if (!map.contains(pair)) {
          map.add(0, pair);
        }
      }
    }
  }

  /**
   * Remove an action's postconditions from the database. Should only be used
   * internally.
   *
   * @param action the action for which the postconditions should be removed
   */
  private synchronized void removePostcondsFromDB(ActionDBEntry action) {
    for (Effect post : action.getPostConditions()) {
      String name = post.getPredicate().getName();
      List<Pair<Predicate, ActionDBEntry>> map;
      log.debug("removing postcond from DB: " + post);

      map = new ArrayList<>(postcondDB.get(name));

      for (Pair<Predicate, ActionDBEntry> pa : map) {
        if (pa.getLeft().instanceOf(post.getPredicate()) && pa.getRight().equals(action)) {
          postcondDB.get(name).remove(pa);
        }
      }
    }
  }

  /**
   * Remove an action from the database
   *
   * @param entry to remove
   */
  public final synchronized void removeAction(ActionDBEntry entry) {
    if (entry == null) {
      log.warn("Attempting to remove null action. Ignoring request.");
    }

    // remove entry all containers
    if (actionDB.containsKey(entry.getType())) {
      actionDB.get(entry.getType()).remove(entry);
      if (actionDB.get(entry.getType()).isEmpty()) {
        actionDB.remove(entry.getType());
      }
      // remove from postcond db
      removePostcondsFromDB(entry);

      // remove from primitive/script set
      if (entry.isPrimitive()) {
        primitives.remove(entry);
      } else {
        scripts.remove(entry);
      }
    }
  }

  /**
   * Move an action into a disabledActions list so it isn't used during goal execution.
   *
   * @param entry to disable
   */
  public final synchronized void disableAction(ActionDBEntry entry) {
    removeAction(entry);
    Utilities.putEntry(disabledActionDB, entry);
  }

  /**
   * Lookup disabled action by type. Returns last added entry if more than one action
   * exists. Returns null if no matching action exists.
   *
   * @param type the type of the action to look up
   * @return the requested action (last added), if found, null otherwise
   */
  public final ActionDBEntry getDisabledAction(String type) {
    ActionDBEntry adb = Utilities.getEntry(disabledActionDB, type);
    if (adb == null) {
      log.warn("[getDisabledAction] Could not find action for type: " + type);
    }
    return adb;
  }

  /**
   * Check if a certain action is available in this goal manager for a particular actor
   *
   * @param goal goal(actor,state) or action(actor,args)
   * @return true if action is in database
   */
  @TRADEService
  @Action
  public Boolean actionExists(Predicate goal) {
    List<ActionDBEntry> actionsWithPostcond;
    if (goal.getName().equals("goal") && goal.size() == 2) {
      actionsWithPostcond = getActionsByEffect(goal.get(0), (Predicate) goal.get(1));
    } else {
      actionsWithPostcond = getActionsByEffect(null, goal);
    }
    List<ActionDBEntry> actionsWithSignature = getActionsBySignature(goal);
    return (!actionsWithPostcond.isEmpty() || !actionsWithSignature.isEmpty());
  }

  /**
   * Lookup by postcondition.
   *
   * @param actor  to specify the actor that must be able to execute the action(s). Can be null.
   * @param effect the postcondition of the entity to look up
   * @return the requested entry, if found, null otherwise
   */
  @TRADEService
  @Action
  public synchronized List<ActionDBEntry> getActionsByEffect(Symbol actor, Predicate effect) {
    log.debug("looking up postcondition");
    String pname = effect.getName();
    List<Pair<Predicate, ActionDBEntry>> map;
    if (postcondDB.containsKey(pname)) {
      map = postcondDB.get(pname);
    } else {
      map = new ArrayList<>();
    }
    return Utilities.filterEntries(effect, map, actor);
  }

  /**
   * Look up action(s) by signature.
   *
   * @param actionSignature
   * @return
   */
  @TRADEService
  @Action
  public synchronized List<ActionDBEntry> getActionsBySignature(Predicate actionSignature) {
    log.debug("looking up action by signature");
    String actionName = actionSignature.getName();
    List<Pair<Predicate, ActionDBEntry>> map = new ArrayList<>();
    if (actionDB.containsKey(actionName)) {
      List<ActionDBEntry> actions = actionDB.get(actionName);
      actions.forEach(action -> action.getSignatureOptions(true)
              .forEach(signature -> map.add(Pair.of(signature, action))));
    }

    if (!map.isEmpty()) {
      Symbol actor = actionSignature.get(0);
      return Utilities.filterEntries(actionSignature, map, actor);
    }

    return new ArrayList<>();
  }

  /**
   * Return all actions (primitives and scripts) in the database.
   *
   * @return
   */
  @TRADEService
  @Action
  public final synchronized Set<ActionDBEntry> getAllActions() {
    Set<ActionDBEntry> allActions = new HashSet<>();

    // add action primitives
    allActions.addAll(primitives);

    // add action scripts
    allActions.addAll(scripts);

    return allActions;
  }

  /**
   * Get primitive set of ActionDBEntries.
   *
   * @return
   */
  public final synchronized Set<ActionDBEntry> getPrimitives() {
    Set<ActionDBEntry> allActions = new HashSet<>();
    allActions.addAll(primitives);
    return allActions;
  }

  /**
   * Get primitive set of ActionDBEntries.
   *
   * @return
   */
  public final synchronized Set<ActionDBEntry> getScripts() {
    Set<ActionDBEntry> allActions = new HashSet<>();
    allActions.addAll(scripts);
    return allActions;
  }

  /**
   * Remove all actions with a certain action signature.
   *
   * @param actionSignature action signature to remove.
   */
  @TRADEService
  @Action
  protected final void removeActionsWithSignature(Predicate actionSignature) {
    List<ActionDBEntry> actions = getActionsBySignature(actionSignature);
    for (ActionDBEntry action : actions) {
      removeAction(action);
    }
  }

  /**
   * Remove all script actions from DB, used to destroy/restart database/gm.
   */
  protected synchronized void removeScripts() {
    List<ActionDBEntry> tmp = new ArrayList<>(scripts); // tmp copy bc actions are removed from scripts during iteration
    tmp.forEach(this::removeAction);
  }

  //TODO:brad:temporarily adding this to restore "food ordering" demo functionality, should be removedm when type checking can occur in action selection
  @TRADEService
  @Action
  public synchronized List<Predicate> getActionSignaturesForName(Symbol actionName) {
    List<Predicate> toReturn = new ArrayList<>();
    List<ActionDBEntry> entries = actionDB.get(actionName.getName());
    if (entries != null) {
      for (ActionDBEntry e : actionDB.get(actionName.getName())) {
        toReturn.add(e.getSignature(true));
      }
    }
    return toReturn;
  }

}