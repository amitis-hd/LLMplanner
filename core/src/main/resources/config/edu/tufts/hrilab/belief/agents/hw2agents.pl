%% agents the system should know about
name(self,self). %% we might want this to be dynamic too
name(self, roboshopper).
name(roboshopper,roboshopper).

actor(amitis).

diarcAgent(self).
diarcAgent(roboshopper).


memberOf(X,X).
memberOf(roboshopper, self).

object(self, agent).
object(roboshopper, agent).
team(self).


/*rules about who the agent is obliged to listen to */
%% supervisors
supervisor(amitis).

%% admin
admin(amitis).
