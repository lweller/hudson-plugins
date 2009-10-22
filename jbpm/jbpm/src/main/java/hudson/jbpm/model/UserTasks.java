/* 
 * Copyright 2008 Tom Huybrechts and hudson.dev.java.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * 	http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing 
 * permissions and limitations under the License.  
 * 
 */
package hudson.jbpm.model;

import java.util.List;

import org.jbpm.taskmgmt.exe.TaskInstance;

import hudson.Extension;
import hudson.jbpm.PluginImpl;
import hudson.widgets.Widget;

@Extension
public class UserTasks extends Widget {

	@Override
	public String getUrlName() {
		return "workflow-tasks";
	}

	public List<TaskInstance> getTasks() {
		return PluginImpl.INSTANCE.getPooledTasks();
	}
	
}
