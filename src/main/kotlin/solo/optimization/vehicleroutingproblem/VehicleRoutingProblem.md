# <https://www.codingame.com/multiplayer/optimization/vehicle-routing-problem>

## <https://en.wikipedia.org/wiki/Vehicle_routing_problem>

## Learning Opportunities

This puzzle can be solved using the following concepts. Practice using these concepts and improve your skills.

Greedy algorithms
Genetic Algorithm
Simulated Annealing

## Statement

### Capacitated Vehicle Routing

In the Vehicle Routing Problem, you must serve a set of customers with a fleet of vehicles.
In the Capacitated variant, vehicles have a limited capacity.
You must find the shortest possible set of tours.

## Rules

The vehicles start from the depot (index 0) and must return to it.
Every customer must be visited exactly once.
For each tour, the sum of the demands of the customers visited must not exceed the capacity of the vehicles.
The distance between two points is Euclidean, rounded to the nearest integer: dist(a, b) = round(sqrt((xa - xb)2 + (ya - yb)2))

## Example

Given the input of the first test case:

5 10 -> n = 5, c = 10
0 0 0 0 -> depot at (0,0) - no demand
1 0 10 3 -> customer 1 at (0,10) demand=3
2 -10 10 3 -> customer 2 at (-10,10) demand=3
3 0 -10 3 -> customer 3 at (0,-10) demand=3
4 10 -10 3 -> customer 4 at (10,-10) demand=3

### Some example outputs in the correct format:

1 2 3;4
The first vehicle goes 0 -> 1 -> 2 -> 3 -> 0. The second vehicle goes 0 -> 4 -> 0.
The distance is dist(0, 1) + dist(1, 2) + dist(2, 3) + dist(3, 0) + dist(0, 4) + dist(4, 0) = 10 + 10 + sqrt(500) + 10 + sqrt(200) + sqrt(200) ≈ 80.6.
4 2 1 3
The first vehicle goes 0 -> 4 -> 2 -> 1 -> 3 -> 0.
This solution is invalid: the sum of demands is 3 + 3 + 3 + 3 > c = 10.
1;2 4;3 2
This solution is invalid: Customer 2 is visited twice.
1;3 4
This solution is invalid: Customer 2 is not visited.
1 2;3 4
This solution is valid and optimal.
The distance is dist(0, 1) + dist(1, 2) + dist(2, 0) + dist(0, 3) + dist(3, 4) + dist(4, 0) = 10 + 10 + sqrt(200) + 10 + 10 + sqrt(200) ≈ 68.3.

## Some Tips

The VRP is a classic NP-Hard problem. Finding an optimal solution is incredibly difficult, so you should use approximation algorithms - time to bring out your favorite metaheuristics!
Have you already solved the Travelling Salesman Problem ? If so, maybe you can reuse your code: every vehicle tour is basically a small TSP.

## About the Test & Validation Instances

The 4 benchmark instances are from the CVRPLib, specifically sets A and M. Their known optima is used to give you the optimality gap. Feel free to use other instances from the library to tune your algorithms.
Validation instances will be similar but different to prevent hard-coded solutions.



## Capacitated Vehicle Routing
In the Vehicle Routing Problem, you must serve a set of customers with a fleet of vehicles.
In the Capacitated variant, vehicles have a limited capacity.
You must find the shortest possible set of tours.

## Rules

The vehicles start from the depot (index 0) and must return to it.
Every customer must be visited exactly once.
For each tour, the sum of the demands of the customers visited must not exceed the capacity of the vehicles.
The distance between two points is Euclidean, rounded to the nearest integer: dist(a, b) = round(sqrt((xa - xb)2 + (ya - yb)2))

## Game Input

The program must first read the given inputs, then output a single line representing the vehicle tours.

### Input

Line 1: an integer n, the number of customers (+1 for the depot)

Line 2: an integer c, the capacity of the vehicles

Next n lines: 4 space-separated integers for each customer/depot

index, the index of the customer (or 0 for the depot)
x, the first coordinate of the customer or depot
y, the second coordinate of the customer or depot
demand, the customer's demand. The depot (index=0) has a demand of 0.

### Output

A single line containing the tours separated by a semicolon.

Each tour must be the indices of the customers separated by a space.

The depot (0) should not be included in the output.

## Constraints

5 <= n <= 200

Response time is limited to 10 seconds.

## Example

Given the input of the first test case:

5 10 -> n = 5, c = 10
0 0 0 0 -> depot at (0,0) - no demand
1 0 10 3 -> customer 1 at (0,10) demand=3
2 -10 10 3 -> customer 2 at (-10,10) demand=3
3 0 -10 3 -> customer 3 at (0,-10) demand=3
4 10 -10 3 -> customer 4 at (10,-10) demand=3

### Some example outputs in the correct format:

1 2 3;4
The first vehicle goes 0 -> 1 -> 2 -> 3 -> 0. The second vehicle goes 0 -> 4 -> 0.
The distance is dist(0, 1) + dist(1, 2) + dist(2, 3) + dist(3, 0) + dist(0, 4) + dist(4, 0) = 10 + 10 + sqrt(500) + 10 + sqrt(200) + sqrt(200) ≈ 80.6.
4 2 1 3
The first vehicle goes 0 -> 4 -> 2 -> 1 -> 3 -> 0.
This solution is invalid: the sum of demands is 3 + 3 + 3 + 3 > c = 10.
1;2 4;3 2
This solution is invalid: Customer 2 is visited twice.
1;3 4
This solution is invalid: Customer 2 is not visited.
1 2;3 4
This solution is valid and optimal.
The distance is dist(0, 1) + dist(1, 2) + dist(2, 0) + dist(0, 3) + dist(3, 4) + dist(4, 0) = 10 + 10 + sqrt(200) + 10 + 10 + sqrt(200) ≈ 68.3.

## Some Tips

The VRP is a classic NP-Hard problem. Finding an optimal solution is incredibly difficult, so you should use approximation algorithms - time to bring out your favorite metaheuristics!
Have you already solved the Travelling Salesman Problem ? If so, maybe you can reuse your code: every vehicle tour is basically a small TSP.

## About the Test & Validation Instances

The 4 benchmark instances are from the CVRPLib, specifically sets A and M. Their known optima is used to give you the optimality gap. Feel free to use other instances from the library to tune your algorithms.
Validation instances will be similar but different to prevent hard-coded solutions.


```text
The vehicle routing problem (VRP) is a combinatorial optimization and integer programming problem which asks "What is the optimal set of routes for a fleet of vehicles to traverse in order to deliver to a given set of customers?" It generalises the travelling salesman problem (TSP). It first appeared in a paper by George Dantzig and John Ramser in 1959,[1] in which the first algorithmic approach was written and was applied to petrol deliveries. Often, the context is that of delivering goods located at a central depot to customers who have placed orders for such goods. The objective of the VRP is to minimize the total route cost. In 1964, Clarke and Wright improved on Dantzig and Ramser's approach using an effective greedy algorithm called the savings algorithm.

Determining the optimal solution to VRP is NP-hard,[2] so the size of problems that can be optimally solved using mathematical programming or combinatorial optimization can be limited. Therefore, commercial solvers tend to use heuristics due to the size and frequency of real world VRPs they need to solve.

VRP has many direct applications in industry. Vendors of VRP routing tools often claim that they can offer cost savings of 5%–30%.[3]

Setting up the problem
The VRP concerns the service of a delivery company. How things are delivered from one or more depots which has a given set of home vehicles and operated by a set of drivers who can move on a given road network to a set of customers. It asks for a determination of a set of routes, S, (one route for each vehicle that must start and finish at its own depot) such that all customers' requirements and operational constraints are satisfied and the global transportation cost is minimized. This cost may be monetary, distance or otherwise.[2]

The road network can be described using a graph where the arcs are roads and vertices are junctions between them. The arcs may be directed or undirected due to the possible presence of one way streets or different costs in each direction. Each arc has an associated cost which is generally its length or travel time which may be dependent on vehicle type.[2]

To know the global cost of each route, the travel cost and the travel time between each customer and the depot must be known. To do this our original graph is transformed into one where the vertices are the customers and depot, and the arcs are the roads between them. The cost on each arc is the lowest cost between the two points on the original road network. This is easy to do as shortest path problems are relatively easy to solve. This transforms the sparse original graph into a complete graph. For each pair of vertices i and j, there exists an arc (i,j) of the complete graph whose cost is written as 
C
i
j
{\displaystyle C_{ij}} and is defined to be the cost of shortest path from i to j. The travel time 
t
i
j
{\displaystyle t_{ij}} is the sum of the travel times of the arcs on the shortest path from i to j on the original road graph.

Sometimes it is impossible to satisfy all of a customer's demands and in such cases solvers may reduce some customers' demands or leave some customers unserved. To deal with these situations a priority variable for each customer can be introduced or associated penalties for the partial or lack of service for each customer given [2]

The objective function of a VRP can be very different depending on the particular application of the result but a few of the more common objectives are:[2]

Minimize the global transportation cost based on the global distance travelled as well as the fixed costs associated with the used vehicles and drivers
Minimize the number of vehicles needed to serve all customers
Least variation in travel time and vehicle load
Minimize penalties for low quality service
Maximize a collected profit/score.
VRP variants

A map showing the relationship between common VRP subproblems.
Several variations and specializations of the vehicle routing problem exist:

Vehicle Routing Problem with Profits (VRPP): A maximization problem where it is not mandatory to visit all customers. The aim is to visit once customers maximizing the sum of collected profits while respecting a vehicle time limit. Vehicles are required to start and end at the depot. Among the most known and studied VRPP, we cite:
The Team Orienteering Problem (TOP) which is the most studied variant of the VRPP,[4][5][6]
The Capacitated Team Orienteering Problem (CTOP),
The TOP with Time Windows (TOPTW).
Vehicle Routing Problem with Pickup and Delivery (VRPPD): A number of goods need to be moved from certain pickup locations to other delivery locations. The goal is to find optimal routes for a fleet of vehicles to visit the pickup and drop-off locations.
Vehicle Routing Problem with LIFO: Similar to the VRPPD, except an additional restriction is placed on the loading of the vehicles: at any delivery location, the item being delivered must be the item most recently picked up. This scheme reduces the loading and unloading times at delivery locations because there is no need to temporarily unload items other than the ones that should be dropped off.
Vehicle Routing Problem with Time Windows (VRPTW): The delivery locations have time windows within which the deliveries (or visits) must be made.
Capacitated Vehicle Routing Problem: CVRP or CVRPTW. The vehicles have a limited carrying capacity of the goods that must be delivered.
Vehicle Routing Problem with Multiple Trips (VRPMT): The vehicles can do more than one route.
Open Vehicle Routing Problem (OVRP): Vehicles are not required to return to the depot.
Inventory Routing Problem (IRP): Vehicles are responsible for satisfying the demands in each delivery point [7]
Multi-Depot Vehicle Routing Problem (MDVRP): Multiple depots exist from which vehicles can start and end.[8]
Vehicle Routing Problem with Transfers (VRPWT): Goods can be transferred between vehicles at specially designated transfer hubs.
Electric Vehicle Routing Problem (EVRP): These are special VRP that take as an extra constraint the battery capacity of electric vehicles into account.
Several software vendors have built software products to solve various VRP problems. Numerous articles are available for more detail on their research and results.

Although VRP is related to the Job Shop Scheduling Problem, the two problems are typically solved using different techniques.[9]

Exact solution methods
There are three main different approaches to modelling the VRP

Vehicle flow formulations—this uses integer variables associated with each arc that count the number of times that the edge is traversed by a vehicle. It is generally used for basic VRPs. This is good for cases where the solution cost can be expressed as the sum of any costs associated with the arcs. However it can't be used to handle many practical applications.[2]
Commodity flow formulations—additional integer variables are associated with the arcs or edges which represent the flow of commodities along the paths travelled by the vehicles. This has only recently been used to find an exact solution.[2]
Set partitioning problem—These have an exponential number of binary variables which are each associated with a different feasible circuit. The VRP is then instead formulated as a set partitioning problem which asks what is the collection of circuits with minimum cost that satisfy the VRP constraints. This allows for very general route costs.[2]
Vehicle flow formulations
The formulation of the TSP by Dantzig, Fulkerson and Johnson was extended to create the two index vehicle flow formulations for the VRP

min
∑
i
∈
V
∑
j
∈
V
c
i
j
x
i
j
{\displaystyle {\text{min}}\sum _{i\in V}\sum _{j\in V}c_{ij}x_{ij}}
subject to

∑
i
∈
V
x
i
j
=
1
∀
j
∈
V
∖
{
0
}
{\displaystyle \sum _{i\in V}x_{ij}=1\quad \forall j\in V\backslash \left\{0\right\}}		1
∑
j
∈
V
x
i
j
=
1
∀
i
∈
V
∖
{
0
}
{\displaystyle \sum _{j\in V}x_{ij}=1\quad \forall i\in V\backslash \left\{0\right\}}		2
∑
i
∈
V
∖
{
0
}
x
i
0
=
K
{\displaystyle \sum _{i\in {V\backslash \left\{0\right\}}}x_{i0}=K}		3
∑
j
∈
V
∖
{
0
}
x
0
j
=
K
{\displaystyle \sum _{j\in {V\backslash \left\{0\right\}}}x_{0j}=K}		4
∑
i
∉
S
∑
j
∈
S
x
i
j
≥
r
(
S
)
,
 
 
∀
S
⊆
V
∖
{
0
}
,
S
≠
∅
{\displaystyle \sum _{i\notin S}\sum _{j\in S}x_{ij}\geq r(S),~~\forall S\subseteq V\setminus \{0\},S\neq \emptyset }		5
x
i
j
∈
{
0
,
1
}
∀
i
,
j
∈
V
{\displaystyle x_{ij}\in \{0,1\}\quad \forall i,j\in V}		6
In this formulation 
c
i
j
{\displaystyle c_{ij}} represents the cost of going from node 
i
{\displaystyle i} to node 
j
{\displaystyle j}, 
x
i
j
{\displaystyle x_{ij}} is a binary variable that has value 
1
{\displaystyle 1} if the arc going from 
i
{\displaystyle i} to 
j
{\displaystyle j} is considered as part of the solution and 
0
{\displaystyle 0} otherwise, 
K
{\displaystyle K} is the number of available vehicles and 
r
(
S
)
{\displaystyle r(S)} corresponds to the minimum number of vehicles needed to serve set 
S
{\displaystyle S}. We are also assuming that 
0
{\displaystyle 0} is the depot node.

Constraints 1 and 2 state that exactly one arc enters and exactly one leaves each vertex associated with a customer, respectively. Constraints 3 and 4 say that the number of vehicles leaving the depot is the same as the number entering. Constraints 5 are the capacity cut constraints, which impose that the routes must be connected and that the demand on each route must not exceed the vehicle capacity. Finally, constraints 6 are the integrality constraints.[2]

One arbitrary constraint among the 
2
|
V
|
{\displaystyle 2|V|} constraints is actually implied by the remaining 
2
|
V
|
−
1
{\displaystyle 2|V|-1} ones so it can be removed. Each cut defined by a customer set 
S
{\displaystyle S} is crossed by a number of arcs not smaller than ⁠
r
(
S
)
{\displaystyle r(S)}⁠(minimum number of vehicles needed to serve set 
S
{\displaystyle S}).[2]

An alternative formulation may be obtained by transforming the capacity cut constraints into generalised subtour elimination constraints (GSECs).

∑
i
∈
S
∑
j
∈
S
x
i
j
≤
|
S
|
−
r
(
S
)
{\displaystyle \sum _{i\in S}\sum _{j\in S}x_{ij}\leq |S|-r(S)}
which imposes that at least ⁠
r
(
S
)
{\displaystyle r(S)}⁠arcs leave each customer set 
S
{\displaystyle S}.[2]

GCECs and CCCs have an exponential number of constraints so it is practically impossible to solve the linear relaxation. A possible way to solve this is to consider a limited subset of these constraints and add the rest if needed. Identification of the needed constraints is done via a separation procedure. Efficient exact separation methods for such constraints (based on mixed integer programming) have been developed.[10]

A different method again is to use a family of constraints which have a polynomial cardinality which are known as the MTZ constraints, they were first proposed for the TSP [11] and subsequently extended by Christofides, Mingozzi and Toth.[12]

u
j
−
u
i
≥
d
j
−
C
(
1
−
x
i
j
)
 
 
 
 
 
 
∀
i
,
j
∈
V
∖
{
0
}
,
i
≠
j
 
 
 
 
s.t. 
d
i
+
d
j
≤
C
{\displaystyle u_{j}-u_{i}\geq d_{j}-C(1-x_{ij})~~~~~~\forall i,j\in V\backslash \{0\},i\neq j~~~~{\text{s.t. }}d_{i}+d_{j}\leq C}
0
≤
u
i
≤
C
−
d
i
 
 
 
 
 
 
∀
i
∈
V
∖
{
0
}
{\displaystyle 0\leq u_{i}\leq C-d_{i}~~~~~~\forall i\in V\backslash \{0\}}
where 
u
i
,
 
i
∈
V
∖
{
0
}
{\displaystyle u_{i},~i\in V\backslash \{0\}} is an additional continuous variable which represents the load left in the vehicle after visiting customer 
i
{\displaystyle i} and 
d
i
{\displaystyle d_{i}} is the demand of customer 
i
{\displaystyle i}. These impose both the connectivity and the capacity requirements. When 
x
i
j
=
0
{\displaystyle x_{ij}=0} constraint then 
i
{\displaystyle i} is not binding' since 
u
i
≤
C
{\displaystyle u_{i}\leq C} and 
u
j
≥
d
j
{\displaystyle u_{j}\geq d_{j}} whereas 
x
i
j
=
1
{\displaystyle x_{ij}=1} they impose that 
u
j
≥
u
i
+
d
j
{\displaystyle u_{j}\geq u_{i}+d_{j}}.

These have been used extensively to model the basic VRP (CVRP) and the VRPB. However, their power is limited to these simple problems. They can only be used when the cost of the solution can be expressed as the sum of the costs of the arc costs. We cannot also know which vehicle traverses each arc. Hence we cannot use this for more complex models where the cost and or feasibility is dependent on the order of the customers or the vehicles used.[2]

Manual versus automatic optimum routing
There are many methods to solve vehicle routing problems manually. For example, optimum routing is a big efficiency issue for forklifts in large warehouses. Some of the manual methods to decide upon the most efficient route are: Largest gap, S-shape, Aisle-by-aisle, Combined and Combined +. While Combined + method is the most complex, thus the hardest to be used by lift truck operators, it is the most efficient routing method. Still the percentage difference between the manual optimum routing method and the real optimum route was on average 13%.[13][14]

Metaheuristic
Due to the difficulty of solving to optimality large-scale instances of vehicle routing problems, a significant research effort has been dedicated to metaheuristics such as Genetic algorithms, Tabu search, Simulated annealing and Adaptive Large Neighborhood Search (ALNS). Some of the most recent and efficient metaheuristics for vehicle routing problems reach solutions within 0.5% or 1% of the optimum for problem instances counting hundreds or thousands of delivery points.[15] These methods are also more robust in the sense that they can be more easily adapted to deal with a variety of side constraints. As such, the application of metaheuristic techniques is often preferred for large-scale applications with complicating constraints and decision sets.
```