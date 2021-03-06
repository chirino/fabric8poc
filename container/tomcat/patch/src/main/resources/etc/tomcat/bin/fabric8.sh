#!/bin/sh

###
# #%L
# Fabric8 :: Container :: Tomcat :: Patch
# %%
# Copyright (C) 2014 Red Hat
# %%
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# #L%
###
################################################################################
#
#    Licensed to the Apache Software Foundation (ASF) under one or more
#    contributor license agreements.  See the NOTICE file distributed with
#    this work for additional information regarding copyright ownership.
#    The ASF licenses this file to You under the Apache License, Version 2.0
#    (the "License"); you may not use this file except in compliance with
#    the License.  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
#
################################################################################

DIRNAME=`dirname "$0"`

# lets check if we are not about to join a fabric
if [ "x$FABRIC8_ZOOKEEPER_URL" = "x" ]; then

  # unless we explicitly disable it, lets auto-start an ensemble
  if [ "x$FABRIC8_ENSEMBLE_AUTO_START" = "x" ]; then
    FABRIC8_ENSEMBLE_AUTO_START=true
    export FABRIC8_ENSEMBLE_AUTO_START
  fi

  # unless we explicitly disable it, lets auto-start the agent
  if [ "x$FABRIC8_AGENT_AUTO_START" = "x" ]; then
    FABRIC8_AGENT_AUTO_START=true
    export FABRIC8_AGENT_AUTO_START
  fi
fi

CATALINA_BASE=`cd "$DIRNAME/.." >/dev/null; pwd`
export LOGGING_CONFIG="-Dlog4j.debug=true -Dlog4j.configuration=file://$CATALINA_BASE/conf/log4j.properties"
export LOGGING_MANAGER="-Dnoop"

echo "Using LOGGING_CONFIG: $LOGGING_CONFIG"
echo "Using LOGGING_MANAGER: $LOGGING_MANAGER"

exec "${DIRNAME}/catalina.sh" "$@"
