/* 
* Copyright (C) 2007, Andrew Butcher

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

* 
*  $Revision: 823 $
*  $Date: 2008-09-14 14:23:43 -0600 (Sun, 14 Sep 2008) $
*  $Author: brian@tannerpages.com $
*  $HeadURL: http://rl-glue.googlecode.com/svn/trunk/src/RL_network_experiment.c $
* 
*/
#ifdef HAVE_CONFIG_H
#include <config.h>
#endif


#include <rlglue/network/RL_network.h>

static int expConnection = 0;

int rlDidExperimentConnect()
{
  return expConnection != 0;
}

void rlCloseExperimentConnection()
{
  rlClose(expConnection);
  expConnection = 0;
}

void rlSetExperimentConnection(int connection)
{
  /* We can't really send a term signal back to the user benchmark,
     they won't know what to do with it. */
  if (rlDidExperimentConnect())
    rlCloseExperimentConnection();

    expConnection = connection;
}
