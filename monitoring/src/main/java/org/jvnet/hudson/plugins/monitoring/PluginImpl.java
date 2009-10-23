package org.jvnet.hudson.plugins.monitoring;

import hudson.Plugin;
import hudson.util.PluginServletFilter;

/**
 * Entry point of the plugin.
 *
 * <p>
 * There must be one {@link Plugin} class in each plugin.
 * See javadoc of {@link Plugin} for more about what can be done on this class.
 *
 * @author Emeric Vernat
 */
public class PluginImpl extends Plugin {

  @Override
  public void start() throws Exception {
    super.start();
	
	// on active les actions syst�mes (gc, heap dump, histogramme m�moire, processus...), sauf si l'administrateur a dit diff�remment
	if (System.getProperty("javamelody.system-actions-enabled") == null) {
		System.setProperty("javamelody.system-actions-enabled", "true");
	}
	// on d�sactive les statistiques sql puisqu'il n'y en aura pas
	if (System.getProperty("javamelody.displayed-counters") == null) {
		System.setProperty("javamelody.displayed-counters", "http,error,log");
	}
	// le r�pertoire de stockage est dans le r�pertoire de hudson au lieu d'�tre dans le r�pertoire temporaire
	// ("/" initial n�cessaire sous windows pour javamelody v1.8.1)
	if (System.getProperty("javamelody.storage-directory") == null && System.getenv("HUDSON_HOME") != null) {
		System.setProperty("javamelody.storage-directory", "/" + System.getenv("HUDSON_HOME") + "/monitoring");
	}
	// google-analytics pour conna�tre le nombre d'installations actives et pour conna�tre les fonctions les plus utilis�es
	if (System.getProperty("javamelody.analytics-id") == null) {
		System.setProperty("javamelody.analytics-id", "UA-1335263-7");
	}
	
	PluginServletFilter.addFilter(new net.bull.javamelody.MonitoringFilter());
	
	// TODO on ne peut pas ajouter aussi un SessionListener comme dans un web.xml ?
	// TODO il faudrait enlever used jdbc connections & active jdbc connections qui ne servent pas pour hudson
	// TODO on pourrait v�rifier si la page "/manage" est appel�e et ajouter un lien "/monitoring" dans le flux html avant <a href="load-statistics">
	// mais pour l'instant on se contentera d'ajouter un lien dans la liste des plugins disponibles ou install�s (index.jelly)
  }
}
