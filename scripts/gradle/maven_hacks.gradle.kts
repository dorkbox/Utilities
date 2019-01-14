///////////////////////////////
// functions to make changing the maven POM easier
@Suppress("UNCHECKED_CAST")
fun org.gradle.api.publish.maven.MavenPom.removeDependenciesByArtifactId(vararg names: String) {
    withXml {
        (asNode().get("dependencies") as List<groovy.util.Node>).forEach {deps ->
            (deps.children() as List<groovy.util.Node>).filter { it ->
                val text = (it.get("artifactId") as List<groovy.util.Node>).firstOrNull()?.text()
                if (text == null) {
                    false
                } else {
                    names.contains(text)
                }
            }.forEach { node->
                node.parent().remove(node)
            }
        }
    }
}
