package org.cloudfoundry.credhub.audit.entities

import org.apache.commons.lang3.builder.EqualsBuilder
import org.cloudfoundry.credhub.audit.OperationDeviceAction
import org.cloudfoundry.credhub.audit.RequestDetails
import java.util.Objects

class GetCredential(credentialName: String, numberOfVersions: Int?, current: Boolean) : RequestDetails {
    var name: String? = credentialName
    var versions: Int? = numberOfVersions
    var current: Boolean? = current

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }

        if (o == null || javaClass != o.javaClass) {
            return false
        }

        val that = o as GetCredential?

        return EqualsBuilder()
            .append(name, that!!.name)
            .append(versions, that.versions)
            .append(current, that.current)
            .isEquals
    }

    override fun hashCode(): Int {
        return Objects.hash(name, versions, current)
    }

    override fun operation(): OperationDeviceAction {
        return OperationDeviceAction.GET
    }
}
