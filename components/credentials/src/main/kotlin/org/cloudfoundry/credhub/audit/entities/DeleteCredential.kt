package org.cloudfoundry.credhub.audit.entities

import org.cloudfoundry.credhub.audit.OperationDeviceAction
import org.cloudfoundry.credhub.audit.RequestDetails

class DeleteCredential(name: String) : RequestDetails {
    var name: String? = name

    override fun operation(): OperationDeviceAction {
        return OperationDeviceAction.DELETE
    }
}
