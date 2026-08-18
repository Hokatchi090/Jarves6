package com.jarvis.assistant

class JarvisModuleManager {

    private val modules = linkedMapOf<String, JarvisModule>()

    init {
        register(JarvisModule("clock", "CLOCK", "CLK", JarvisModuleType.CLOCK))
        register(JarvisModule("apps", "APPLICATIONS", "APPS", JarvisModuleType.APPS))
        register(JarvisModule("system", "SYSTEM", "SYS", JarvisModuleType.SYSTEM))
        register(JarvisModule("map", "MAP", "MAP", JarvisModuleType.MAP))
    }

    fun register(module: JarvisModule) {
        modules[module.id] = module
    }

    fun remove(id: String) {
        modules.remove(id)
    }

    fun get(id: String): JarvisModule? {
        return modules[id]
    }

    fun all(): List<JarvisModule> {
        return modules.values.toList()
    }

    fun enabled(): List<JarvisModule> {
        return modules.values.filter { it.enabled }
    }

    fun setEnabled(id: String, enabled: Boolean) {
        modules[id]?.enabled = enabled
    }
}
