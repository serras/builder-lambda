package com.serranofp.builder.lambda.ir

import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.FqName

val IrSymbol.fqNameWhenAvailable: FqName?
    get() = if (isBound) (owner as IrDeclarationWithName?)?.fqNameWhenAvailable
    else (signature as? IdSignature.CommonSignature)?.let { FqName("${it.packageFqName}.${it.declarationFqName}") }

val IrDeclarationWithName.fqNameWhenAvailable: FqName?
    get() {
        val sb = StringBuilder()
        return if (computeFqNameString(this, sb)) FqName(sb.toString()) else null
    }

private fun computeFqNameString(declaration: IrDeclarationWithName, result: StringBuilder): Boolean =
    computeFqNameString(declaration, result, declaration.parent)

private fun computeFqNameString(
    declaration: IrDeclarationWithName,
    result: StringBuilder,
    parent: IrDeclarationParent,
): Boolean {
    when {
        parent is IrDeclarationWithName -> {
            if (!computeFqNameString(parent, result)) return false
        }
        parent is IrPackageFragment -> {
            val packageFqName = parent.packageFqName
            if (!packageFqName.isRoot) result.append(packageFqName)
        }
        else -> return false
    }
    if (result.isNotEmpty()) result.append('.')
    result.append(declaration.name.asString())
    return true
}