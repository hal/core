#!/usr/bin/env bash

ROOT="$PWD"
SCRIPT=`basename $0`
ARGS=$#
COMMAND=$1
SUB_COMMAND=$2

function usage {
    echo -e "Usage: $SCRIPT <help|clean|info|push|pull>\n"
    echo -e "Zanata wrapper script to push and pull the translatable resources to and from Zanata.\n"
    echo " help                        Shows this help"
    echo " clean                       Removes all temporary files"
    echo " info                        Displays details about the translatable resources such as"
    echo "                             number of constants, messages and preview files."
    echo " push <all|bundles|previews> Pushes the specified resources to Zanata."
    echo "                             The resources are first copied to target/zanata/push"
    echo " pull <all|bundles|previews> Pulls the specified resources from Zanata."
    echo "                             The resources are pulled to target/zanata/pull"
    exit 1
}

function clean {
    rm -rf target/zanata
}

function info {
    find src/main/java/org/jboss/as/console/client/core -name "UIConstants*.properties" | xargs wc -l
    find src/main/java/org/jboss/as/console/client/core -name "UIMessages*.properties" | xargs wc -l
    find src/main/java/org/jboss/as/console/client/preview -name "*.html" | xargs wc -l
}

function verifyPushPull {
    if [[ ${ARGS} -ne 2 ]]
    then
        usage
    fi
    case ${SUB_COMMAND} in
        "all")
            ;;
        "bundles")
            ;;
        "previews")
            ;;
        *)
            usage
            ;;
    esac
}

function push {
    if [[ "$SUB_COMMAND" == "all" ]]
    then
        pushBundles
        pushPreviews
    elif [[ "$SUB_COMMAND" == "bundles" ]]
    then
        pushBundles
    elif [[ "$SUB_COMMAND" == "previews" ]]
    then
        pushPreviews
    else
        usage
    fi
}

function pushBundles {
    mkdir -p target/zanata/push/bundles
    cp src/main/zanata/hal-resource-bundles/zanata.xml target/zanata/push/bundles
    cp src/main/java/org/jboss/as/console/client/core/UIConstants*.properties target/zanata/push/bundles
    cp src/main/java/org/jboss/as/console/client/core/UIMessages*.properties target/zanata/push/bundles
    cd target/zanata/push/bundles
    zanata-cli push --batch-mode
    cd "${ROOT}"
}

function pushPreviews {
    mkdir -p target/zanata/push/previews
    mkdir -p target/zanata/push/previews/content
    mkdir -p target/zanata/push/previews/roles
    cp src/main/zanata/hal-html-previews/zanata.xml target/zanata/push/previews
    cp src/main/java/org/jboss/as/console/client/preview/*.html target/zanata/push/previews
    cp src/main/java/org/jboss/as/console/client/preview/content/*.html target/zanata/push/previews/content
    cp src/main/java/org/jboss/as/console/client/preview/roles/*.html target/zanata/push/previews/roles
    cd target/zanata/push/previews
    rm -rf *_ja.html *_zh-Hans.html
    rm -rf content/*_ja.html content/*_zh-Hans.html
    rm -rf roles/*_ja.html roles/*_zh-Hans.html
    zanata-cli push --batch-mode --file-types "HTML[html]"
    cd "${ROOT}"
}

function pull {
    if [[ "$SUB_COMMAND" == "all" ]]
    then
        pullBundles
        pullPreviews
    elif [[ "$SUB_COMMAND" == "bundles" ]]
    then
        pullBundles
    elif [[ "$SUB_COMMAND" == "previews" ]]
    then
        pullPreviews
    else
        usage
    fi
}

function pullBundles {
    mkdir -p target/zanata/pull/bundles
    cp src/main/zanata/hal-resource-bundles/zanata.xml target/zanata/pull/bundles
    cd target/zanata/pull/bundles
    zanata-cli pull --batch-mode
    cd "${ROOT}"
}

function pullPreviews {
    mkdir -p target/zanata/pull/previews
    mkdir -p target/zanata/pull/previews/content
    mkdir -p target/zanata/pull/previews/roles
    cp src/main/zanata/hal-html-previews/zanata.xml target/zanata/pull/previews
    cd target/zanata/pull/previews
    zanata-cli pull --batch-mode
    cd "${ROOT}"
}

# Verify Zanata client is available
command -v zanata-cli >/dev/null 2>&1 || { echo >&2 "Zanata client not found. Follow the instructions at http://zanata-client.readthedocs.org/en/latest/#zanata-command-line-client to install the client."; exit 1; }

# Check
if [[ ${ARGS} -eq 0 ]]
then
  usage
fi

# and parse arguments
case ${COMMAND} in
    "help")
        usage
        ;;
    "clean")
        clean
        ;;
    "info")
        info
        ;;
    "push")
        verifyPushPull
        push
        ;;
    "pull")
        verifyPushPull
        pull
        ;;
    *)
        usage
        ;;
esac
