# incanter-demo

A Clojure library designed to demonstrate Incanter statistics.
The code is taken from David Edgar Liebke's talk slides at:
http://data-sorcery.org/2010/02/11/data-sorcery-pt1/
The code has been updated slightly for Incanter 1.5.3 which is the latest
release.


## Usage

All the code is in core.clj and is intended to be executed line by line,
interactively in the repl.

For the MongoDB example, the code assumes a running instance of mongod.

Datasets are created and saved in an order different to David's presentation
so that they are written before they are read.

## License

Copyright © 2013 David Edgar Liebke, Simon Holgate

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
