# DistrSystm_FloodSim

A Java-based exploration of core **Distributed Systems** principles through a **Flooding Broadcast** simulation. Harnessing **parallel programming** and **message-passing** paradigms, this project models a network of autonomous nodes collaborating to reliably disseminate information.


## Highlights

- **Distributed Architecture**: Each node operates as an independent Java thread, embodying real-world process concurrency.  
- **Parallel Message Passing**: Utilizes asynchronous send/receive loops for high-throughput broadcast across the topology.  
- **Duplicate Suppression**: Implements sequence-number tagging and per-message tracking to guarantee exactly-once delivery semantics.  
- **Topology Configuration**: Text-driven network definitions allow rapid prototyping of arbitrary node graphs and communication patterns.  

