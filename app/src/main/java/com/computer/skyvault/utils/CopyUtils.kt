//package com.computer.skyvault.utils
//
//import android.util.Log
//import kotlin.reflect.KClass
//import kotlin.reflect.full.primaryConstructor
//import kotlin.reflect.jvm.isAccessible
//
//object CopyUtils {
//    private const val TAG = "CopyUtils"
//
//    /**
//     * 将一个类型的对象列表复制到另一个类型的对象列表中
//     *
//     * @param sList 源对象列表
//     * @param tClass 目标对象的 class
//     * @param T 目标对象类型
//     * @param S 源对象类型
//     * @return 目标对象列表
//     */
//    fun <T : Any, S : Any> copyList(sList: List<S>, tClass: Class<T>): List<T> {
//        return sList.mapNotNull { s ->
//            try {
//                val t = tClass.getDeclaredConstructor().newInstance()
//                copyProperties(s, t)
//                t
//            } catch (e: Exception) {
//                Log.e(TAG, "实例化对象失败: ${tClass.simpleName}", e)
//                null
//            }
//        }
//    }
//
//    /**
//     * 将一个对象复制到另一个类型的对象中
//     *
//     * @param s 源对象
//     * @param tClass 目标对象的类类型
//     * @param T 目标对象类型
//     * @param S 源对象类型
//     * @return 目标对象
//     */
//    fun <T : Any, S : Any> copy(s: S, tClass: Class<T>): T? {
//        return try {
//            val t = tClass.getDeclaredConstructor().newInstance()
//            copyProperties(s, t)
//            t
//        } catch (e: Exception) {
//            Log.e(TAG, "实例化对象失败: ${tClass.simpleName}", e)
//            null
//        }
//    }
//
//    /**
//     * 使用Kotlin反射的版本
//     */
//    fun <T : Any, S : Any> copyKt(s: S, tClass: KClass<T>): T? {
//        return try {
//            val constructor = tClass.primaryConstructor?.apply { isAccessible = true }
//            val t = constructor?.call() ?: throw IllegalStateException("No primary constructor found")
//            copyProperties(s, t)
//            t
//        } catch (e: Exception) {
//            Log.e(TAG, "实例化对象失败: ${tClass.simpleName}", e)
//            null
//        }
//    }
//
//    /**
//     * 内联函数版本，避免显式传递Class对象
//     */
//    inline fun <reified T : Any, S : Any> copyReified(s: S): T? {
//        return try {
//            val t = T::class.java.getDeclaredConstructor().newInstance()
//            copyProperties(s, t)
//            t
//        } catch (e: Exception) {
//            Log.e(TAG, "实例化对象失败: ${T::class.simpleName}", e)
//            null
//        }
//    }
//
//    /**
//     * 内联函数版本，避免显式传递Class对象
//     */
//    inline fun <reified T : Any, S : Any> copyListReified(sList: List<S>): List<T> {
//        return sList.mapNotNull { s ->
//            try {
//                val t = T::class.java.getDeclaredConstructor().newInstance()
//                copyProperties(s, t)
//                t
//            } catch (e: Exception) {
//                Log.e(TAG, "实例化对象失败: ${T::class.simpleName}", e)
//                null
//            }
//        }
//    }
//
//    /**
//     * 复制属性（替代Spring的BeanUtils.copyProperties）
//     * 这是一个简单的实现，可以根据需要扩展
//     */
//    private fun copyProperties(source: Any, target: Any) {
//        try {
//            val sourceClass = source::class.java
//            val targetClass = target::class.java
//
//            val sourceFields = sourceClass.declaredFields
//            val targetFields = targetClass.declaredFields.associateBy { it.name }
//
//            for (sourceField in sourceFields) {
//                sourceField.isAccessible = true
//                val targetField = targetFields[sourceField.name]
//
//                if (targetField != null &&
//                    targetField.type.isAssignableFrom(sourceField.type)) {
//                    targetField.isAccessible = true
//                    val value = sourceField.get(source)
//                    targetField.set(target, value)
//                }
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "复制属性失败", e)
//        }
//    }
//
//    /**
//     * 使用MapStruct或手动映射的替代方案
//     */
//    fun <T : Any, S : Any> copyListWithMapper(
//        sList: List<S>,
//        mapper: (S) -> T
//    ): List<T> {
//        return sList.mapNotNull { s ->
//            try {
//                mapper(s)
//            } catch (e: Exception) {
//                Log.e(TAG, "映射对象失败: ${s::class.simpleName}", e)
//                null
//            }
//        }
//    }
//
//    /**
//     * 使用扩展函数简化调用
//     */
//    fun <T : Any, S : Any> List<S>.copyTo(tClass: Class<T>): List<T> {
//        return copyList(this, tClass)
//    }
//
//    /**
//     * 使用扩展函数简化调用
//     */
//    fun <T : Any, S : Any> S.copyTo(tClass: Class<T>): T? {
//        return copy(this, tClass)
//    }
//}