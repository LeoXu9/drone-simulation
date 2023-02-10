# drone-simulation

You have been asked to help the School of Informatics investigate an idea for a service which should allow
the students in the School to make the most of their lunch hour where they take a much-needed break from
lectures, studying, labs, and practicals like this one! The School of Informatics is considering creating a
service where students can place a lunch order for sandwiches and drinks and have these delivered to them
while they take a break in Bristo Square or George Square. Lunches will be delivered by an autonomous
airborne drone which will travel to sandwich shops in the University Central Area to collect the lunch items
in a lunch order, and then fly to a chosen location to deliver these to the student who placed the order. It is
hoped that this service will allow students to make the most of their lunch break because they will not need
to queue at the shops and collect their lunch in person. In addition, the service could be helpful to new
students who have just joined the School and have not yet got their bearings and do not know the sandwich
shops near the Central Area.


The idea for a drone-based sandwich delivery service has not been universally popular. Some of the schools
in the University Central Area think that the potential for error in deliveries is too great and they do not
want to deal with problems such as drones crashing into their building, or getting stranded on the roof of
the building, or dropping sandwiches and drinks onto their staff and students. It is expected that some of
these schools will define a “no-fly zone” consisting of their buildings. The drone will therefore have to plan
its routes so that it does not fly over buildings in the no-fly zone.


Ordering the lunches is relatively straightforward. The School of Informatics can easily create an online
system to take student lunch orders in the morning and add these to a database of orders to be delivered at
lunchtime. What is less clear is whether or not it is feasible for the drone to fulfil these orders, given that (i)
the service is expected to be popular with a lot of lunch orders being placed each day, (ii) only one drone
is available for making the deliveries, (iii) the drone cannot carry more than one order at a time (to avoid
delivering the wrong order to the wrong person), (iv) the drone must avoid buildings in the no-fly zone, and
(v) the drone can only fly for a limited time before its battery will run out and it will need to be recharged.
Recharging is a slow process which means that the drone will no longer be in service that lunchtime.


Your task is to devise and implement an algorithm to control the flight of the drone as it makes its deliveries
while respecting the constraints on drone movement specified in this document. You will be provided with
some synthetic test data representing typical lunch orders and other data about the service such as the
details of the sandwich shops which are participating in the scheme, the menus for these shops, and the
location of the drop-off points where deliveries can be made. This information will come in the form of
a database (for the order information) and a website (for the rest of the information). It is important to
stress that the information in the test data which you will be given only represents the current best guess
at what the elements of the drone service will be when it is operational, and the service in practice might
use different shops or it might deliver to different drop-off points. For this reason, your solution must be
data-driven. That is, it must read the information from the database and the website and particular shops
or particular drop-off points or other details must not be hardcoded in your application, except where it is
explicitly stated in this document that it is acceptable to do so.


Your implementation of the drone control software should be considered to be a prototype only in the sense
that you should think that it is being created with the intention of passing it on to a team of software developers and student volunteers in the School of Informatics who will maintain and develop it in the months
and years ahead when the drone lunch delivery is operational. For this reason, the clarity and readability of
your code is important; you need to produce code which can be read and understood by others.
