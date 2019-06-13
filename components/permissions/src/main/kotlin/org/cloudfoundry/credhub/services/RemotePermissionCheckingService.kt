package org.cloudfoundry.credhub.services

import org.cloudfoundry.credhub.PermissionOperation
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Profile("remote")
class RemotePermissionCheckingService() : PermissionCheckingService{
    override fun hasPermission(user: String, credentialName: String, permission: PermissionOperation): Boolean {
        return true
    }

    override fun hasPermission(user: String, permissionGuid: UUID, permission: PermissionOperation): Boolean {
        return true
    }

    override fun hasPermissions(user: String, path: String, permissions: List<PermissionOperation>): Boolean {
        return true
    }

    override fun userAllowedToOperateOnActor(actor: String?): Boolean {
        return true
    }

    override fun userAllowedToOperateOnActor(guid: UUID): Boolean {
        return true
    }

    override fun findAllPathsByActor(actor: String): Set<String> {
        TODO("not implemented")
    }

}
