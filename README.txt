The agent uses the GridClient and the SensoryPacket classes. 
It is best run when placed in the src folder of the maeden environment (maeden/src)

It can be run from command line after the maeden environment has been started and a world selected,
and complied using cmd: java SRAgent

We didn't have any alternate ideas for use in our agent. We just threw all the ideas we had
into the agent as decisions it could make. One idea we had that we decided not to implement
was a more agressive panic mode. Our current one just hugs the wall at certain energy levels 
which we used to determine our agent is stuck. The other idea we had was to have it pick random 
directions opposite of the heading (smell) of the cheese, again using the energy. This version would
use a much lower level, and implement along with hug wall method, somewhere below 40% and end around 30%.
This would be implemented with the hop that the agent might end up in a "better" position that it had 
started. After which it would return to normal decisions as if that was the starting point. 

Our approach was to implement a simple agent that followed the smell of the cheese and see how many 
world it completed and implement more sophisticated decisions as obsticles were introduced. 
When our agent got to a point where it would get "stuck" we added the idea of hugging the wall
as a panic mode, which improved the performance of the agent as it allowed the agent to possibly
find a "hole" in the wall it was stuck on. 