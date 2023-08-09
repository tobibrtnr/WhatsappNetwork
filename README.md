# Create your own Graph of your WhatsApp Social Network

With this project, you are able to generate a edge map out of your WhatsApp groups. This map can be used in applications, such as Gephi, to generate your own personal social network graph.

![example social network graph](/doc/example.png)

## Getting Started

This script is written in Java and organized as a Maven project. You can compile and run it as a command-line program or execute it just in your IDE. The starting point is *WhatsappDBParser.java*.

In order to generate the graph, you need your **decrypted** *messages.db* file and optionally the *wa.db* file. You will likely need to root your phone or use the WSA. Helpful projects:

- https://github.com/ElDavoo/wa-crypt-tools
- https://github.com/tiann/KernelSU

## Running the Script

When running the program, place the decrypted files in the root folder. You will have to enter some information:

- Name of the decrypted messages file
- Your own phone number without + or spaces (e.g. 491605629294) to exclude it from the graph, as you would be connected to all other numbers
- Name of the wa.db file (optional)
- Wether to include numbers / names or use anonymous ids (for publishing the graph)
- A list of groups that should be excluded and seperated by semicolons (optional, e.g. "Group1; othername; Bad Group")
- Name of the edge file that will be generated
- Wether to export the edges as .json or .csv

## Using the results

The .csv file can be directly imported into Gephi. You can then use a layout algorithm to place the nodes and generate modularity classes in order to color the nodes. 

The .json file can, for example, be used in a npm package like force-graph. Please note that due to the size of the graph, this can be very laggy.

**An example can be found here: https://www.tobibrtnr.de/wa-network/**

Please note that this script is far from perfect and may contain errors. Have fun anyway 🙂