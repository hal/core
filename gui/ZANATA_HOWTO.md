# Zanata Guide

## Installation

Follow the steps at http://zanata-client.readthedocs.org/en/latest/#zanata-command-line-client to install the Zanata client on your machine. 

## Usage

Use the script `zanata.sh` to interact with Zanata. The script is a wrapper around the Zanata client and is used to push and pull the translatable resources to and from Zanata. It accepts the following commands:
 
- `help` - Shows the available options and a short description.
- `info` - Displays details about the translatable resources such as number of constants, messages and preview files.
- `push <all|bundle|preview>` - Pushes the specified resources to Zanata. The resources are first copied to `target/zanata/push` before the're pushed to Zanata.
- `pull <all|bundle|preview>` - Pulls the specified resources from Zanata. The resources are pulled to `target/zanata/pull`.  

