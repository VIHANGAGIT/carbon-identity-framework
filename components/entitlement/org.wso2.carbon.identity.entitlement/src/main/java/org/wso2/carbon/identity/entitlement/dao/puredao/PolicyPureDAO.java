/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.entitlement.dao.puredao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.balana.AbstractPolicy;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.database.utils.jdbc.NamedPreparedStatement;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.entitlement.EntitlementException;
import org.wso2.carbon.identity.entitlement.EntitlementUtil;
import org.wso2.carbon.identity.entitlement.dto.AttributeDTO;
import org.wso2.carbon.identity.entitlement.dto.PolicyDTO;
import org.wso2.carbon.identity.entitlement.dto.PolicyStoreDTO;
import org.wso2.carbon.identity.entitlement.pap.PAPPolicyReader;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.EntitlementTableColumns.ATTRIBUTE_ID;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.EntitlementTableColumns.ATTRIBUTE_VALUE;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.EntitlementTableColumns.CATEGORY;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.EntitlementTableColumns.DATA_TYPE;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.EntitlementTableColumns.EDITOR_DATA;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.EntitlementTableColumns.EDITOR_DATA_ORDER;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.EntitlementTableColumns.IS_ACTIVE;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.EntitlementTableColumns.IS_IN_PAP;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.EntitlementTableColumns.IS_IN_PDP;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.EntitlementTableColumns.LAST_MODIFIED_TIME;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.EntitlementTableColumns.LAST_MODIFIED_USER;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.EntitlementTableColumns.POLICY;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.EntitlementTableColumns.POLICY_EDITOR;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.EntitlementTableColumns.POLICY_ID;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.EntitlementTableColumns.POLICY_ORDER;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.EntitlementTableColumns.POLICY_TYPE;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.EntitlementTableColumns.REFERENCE;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.EntitlementTableColumns.SET_REFERENCE;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.EntitlementTableColumns.TENANT_ID;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.EntitlementTableColumns.VERSION;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.CREATE_PAP_POLICY_ATTRIBUTES_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.CREATE_PAP_POLICY_EDITOR_DATA_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.CREATE_PAP_POLICY_REFS_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.CREATE_PAP_POLICY_SET_REFS_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.CREATE_PAP_POLICY_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.DELETE_PAP_POLICY_BY_VERSION_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.DELETE_PAP_POLICY_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.DELETE_POLICY_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.DELETE_POLICY_VERSION_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.DELETE_PUBLISHED_VERSIONS_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.DELETE_UNPUBLISHED_POLICY_VERSIONS_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.DELETE_UNUSED_POLICY_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.GET_ACTIVE_STATUS_AND_ORDER_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.GET_ALL_PAP_POLICIES_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.GET_ALL_PDP_POLICIES_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.GET_LATEST_POLICY_VERSION_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.GET_PAP_POLICY_BY_VERSION_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.GET_PAP_POLICY_EDITOR_DATA_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.GET_PAP_POLICY_IDS_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.GET_PAP_POLICY_META_DATA_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.GET_PAP_POLICY_REFS_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.GET_PAP_POLICY_SET_REFS_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.GET_PAP_POLICY_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.GET_PDP_POLICY_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.GET_POLICY_PAP_PRESENCE_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.GET_POLICY_PDP_PRESENCE_BY_VERSION_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.GET_POLICY_PDP_PRESENCE_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.GET_POLICY_VERSIONS_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.GET_PUBLISHED_POLICY_VERSION_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.PUBLISH_POLICY_VERSION_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.RESTORE_ACTIVE_STATUS_AND_ORDER_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.UPDATE_ACTIVE_STATUS_SQL;
import static org.wso2.carbon.identity.entitlement.dao.DAOConstants.SQLQueries.UPDATE_ORDER_SQL;

import static java.time.ZoneOffset.UTC;

/**
 * This class handles the JDBC operations related to the policies.
 */
public class PolicyPureDAO {

    private static final Log LOG = LogFactory.getLog(PolicyPureDAO.class);
    private static final String IS_IN_PDP_1 = "IS_IN_PDP_1";
    private static final boolean IN_PAP = true;
    private static final boolean IN_PDP = true;
    private static final boolean INACTIVE = false;
    private static final int DEFAULT_POLICY_ORDER = 0;
    private static final String ERROR_RETRIEVING_PAP_POLICY =
            "Error while retrieving entitlement policy %s from the PAP policy store";

    /**
     * DAO method to insert a policy to PAP.
     *
     * @param policy policy.
     */
    public void insertPolicy(PolicyDTO policy, int tenantId) throws EntitlementException {

        Connection connection = IdentityDatabaseUtil.getDBConnection(true);
        try {

            insertPolicy(connection, policy, tenantId);
            insertPolicyReferences(connection, policy, tenantId);
            if (policy.getAttributeDTOs() != null && EntitlementUtil.isPolicyMetadataStoringEnabled()) {
                insertPolicyAttributes(connection, policy, tenantId);
            }
            insertPolicyEditorData(connection, policy, tenantId);
            IdentityDatabaseUtil.commitTransaction(connection);

        } catch (SQLException e) {
            IdentityDatabaseUtil.rollbackTransaction(connection);
            throw new EntitlementException("Error while adding or updating entitlement policy in policy store", e);
        } finally {
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }

    /**
     * DAO method to delete the given policy version from the PAP.
     *
     * @param policyId policyId.
     * @param version  version.
     * @throws EntitlementException throws, if fails.
     */
    public void deletePolicy(String policyId, int version, int tenantId) throws EntitlementException {

        Connection connection = IdentityDatabaseUtil.getDBConnection(true);

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Removing policy version %s %s", policyId, version));
        }
        try (NamedPreparedStatement findPDPPresencePrepStmt = new NamedPreparedStatement(connection,
                GET_POLICY_PDP_PRESENCE_BY_VERSION_SQL);
             NamedPreparedStatement removePolicyFromPAPPrepStmt = new NamedPreparedStatement(connection,
                     DELETE_PAP_POLICY_BY_VERSION_SQL);
             NamedPreparedStatement removePolicyPrepStmt = new NamedPreparedStatement(connection,
                     DELETE_POLICY_VERSION_SQL)) {

            // Find whether the policy is published or not
            findPDPPresencePrepStmt.setBoolean(IS_IN_PDP, IN_PDP);
            findPDPPresencePrepStmt.setString(POLICY_ID, policyId);
            findPDPPresencePrepStmt.setInt(VERSION, version);
            findPDPPresencePrepStmt.setInt(TENANT_ID, tenantId);
            try (ResultSet resultSet = findPDPPresencePrepStmt.executeQuery()) {

                if (resultSet.next()) {
                    // Remove the policy version from the PAP (It is still present in PDP)
                    removePolicyFromPAPPrepStmt.setBoolean(IS_IN_PAP, !IN_PAP);
                    removePolicyFromPAPPrepStmt.setString(POLICY_ID, policyId);
                    removePolicyFromPAPPrepStmt.setInt(VERSION, version);
                    removePolicyFromPAPPrepStmt.setInt(TENANT_ID, tenantId);
                    removePolicyFromPAPPrepStmt.executeUpdate();
                } else {
                    // Remove the policy version from the database
                    removePolicyPrepStmt.setString(POLICY_ID, policyId);
                    removePolicyPrepStmt.setInt(VERSION, version);
                    removePolicyPrepStmt.setInt(TENANT_ID, tenantId);
                    removePolicyPrepStmt.executeUpdate();
                }
            }
            IdentityDatabaseUtil.commitTransaction(connection);

        } catch (SQLException e) {
            IdentityDatabaseUtil.rollbackTransaction(connection);
            throw new EntitlementException(String.format("Error while removing policy version %s %s from PAP policy " +
                    "store", policyId, version), e);
        } finally {
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }

    /**
     * DAO method to get a policy from PAP.
     *
     * @param policyId policyId.
     * @throws EntitlementException throws, if fails.
     */
    public PolicyDTO getPAPPolicy(String policyId, int tenantId) throws EntitlementException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            try (NamedPreparedStatement prepStmt = new NamedPreparedStatement(connection, GET_PAP_POLICY_SQL)) {
                prepStmt.setBoolean(IS_IN_PAP, IN_PAP);
                prepStmt.setString(POLICY_ID, policyId);
                prepStmt.setInt(TENANT_ID, tenantId);

                try (ResultSet policy = prepStmt.executeQuery()) {
                    if (policy.next()) {
                        return getPolicyDTO(policy, connection);
                    }
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new EntitlementException(String.format(ERROR_RETRIEVING_PAP_POLICY, policyId), e);
        }
    }

    /**
     * DAO method to get all PAP policies.
     *
     * @param tenantId tenant ID.
     * @return list of policy DTOs.
     */
    public List<PolicyDTO> getAllPAPPolicies(int tenantId) throws EntitlementException {

        List<PolicyDTO> policyDTOs = new ArrayList<>();
        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            try (NamedPreparedStatement prepStmt = new NamedPreparedStatement(connection, GET_ALL_PAP_POLICIES_SQL)) {
                prepStmt.setBoolean(IS_IN_PAP, IN_PAP);
                prepStmt.setInt(TENANT_ID, tenantId);

                try (ResultSet policies = prepStmt.executeQuery()) {
                    while (policies.next()) {
                        policyDTOs.add(getPolicyDTO(policies, connection));
                    }
                }
            }
        } catch (SQLException e) {
            throw new EntitlementException("Error while retrieving entitlement policies from the PAP policy store", e);
        }
        return policyDTOs;
    }

    /**
     * DAO method to get the latest policy version.
     *
     * @param policyId policy ID.
     * @param tenantId tenant ID.
     * @throws EntitlementException throws, if fails.
     */
    public String getLatestPolicyVersion(String policyId, int tenantId) throws EntitlementException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            try (NamedPreparedStatement prepStmt = new NamedPreparedStatement(connection,
                    GET_LATEST_POLICY_VERSION_SQL)) {
                prepStmt.setBoolean(IS_IN_PAP, IN_PAP);
                prepStmt.setString(POLICY_ID, policyId);
                prepStmt.setInt(TENANT_ID, tenantId);

                try (ResultSet latestVersion = prepStmt.executeQuery()) {
                    if (latestVersion.next()) {
                        return String.valueOf(latestVersion.getInt(VERSION));
                    }
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new EntitlementException(String.format("Error retrieving the latest version of the policy %s",
                    policyId), e);
        }
    }

    /**
     * DAO method to get the policy by version.
     *
     * @param policyId policy ID.
     * @param version  version.
     * @param tenantId tenant ID.
     * @throws EntitlementException throws, if fails.
     */
    public PolicyDTO getPapPolicyByVersion(String policyId, String version, int tenantId) throws EntitlementException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            try (NamedPreparedStatement prepStmt = new NamedPreparedStatement(connection,
                    GET_PAP_POLICY_BY_VERSION_SQL)) {
                prepStmt.setBoolean(IS_IN_PAP, IN_PAP);
                prepStmt.setString(POLICY_ID, policyId);
                prepStmt.setInt(VERSION, Integer.parseInt(version));
                prepStmt.setInt(TENANT_ID, tenantId);

                try (ResultSet policy = prepStmt.executeQuery()) {
                    if (policy.next()) {
                        return getPolicyDTO(policy, connection);
                    } else {
                        throw new EntitlementException(
                                String.format("No policy with the given policyID %s and version %s exists", policyId,
                                        version));
                    }
                }
            }
        } catch (SQLException e) {
            throw new EntitlementException(String.format(ERROR_RETRIEVING_PAP_POLICY, policyId), e);
        }
    }

    /**
     * DAO method to get all the versions of the policy.
     *
     * @param policyId policy ID.
     * @param tenantId tenant ID.
     * @return latest version of the policy.
     */
    public List<String> getPolicyVersions(String policyId, int tenantId) {

        List<String> versions = new ArrayList<>();

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            try (NamedPreparedStatement prepStmt = new NamedPreparedStatement(connection, GET_POLICY_VERSIONS_SQL)) {
                prepStmt.setString(POLICY_ID, policyId);
                prepStmt.setInt(TENANT_ID, tenantId);

                try (ResultSet versionsSet = prepStmt.executeQuery()) {
                    while (versionsSet.next()) {
                        versions.add(String.valueOf(versionsSet.getInt(VERSION)));
                    }
                }
            }
        } catch (SQLException e) {
            LOG.error(String.format("Error while retrieving policy versions for policy %s", policyId), e);
        }
        return versions;
    }

    /**
     * DAO method to get PAP policy ids.
     *
     * @param tenantId tenant ID.
     * @return list of policy IDs.
     * @throws EntitlementException If an error occurs.
     */
    public List<String> getPAPPolicyIds(int tenantId) throws EntitlementException {

        List<String> policies = new ArrayList<>();

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            try (NamedPreparedStatement prepStmt = new NamedPreparedStatement(connection, GET_PAP_POLICY_IDS_SQL)) {
                prepStmt.setBoolean(IS_IN_PAP, IN_PAP);
                prepStmt.setInt(TENANT_ID, tenantId);

                try (ResultSet policyIds = prepStmt.executeQuery()) {
                    while (policyIds.next()) {
                        policies.add(policyIds.getString(POLICY_ID));
                    }
                    if (policies.isEmpty()) {
                        LOG.debug("No PAP policies found");
                    }
                    return policies;
                }
            }
        } catch (SQLException e) {
            throw new EntitlementException(
                    "Error while retrieving entitlement policy identifiers from PAP policy store", e);
        }
    }

    /**
     * DAO method to delete a policy from PAP.
     *
     * @param policyId policy ID.
     * @param tenantId tenant ID.
     * @throws EntitlementException If an error occurs.
     */
    public void deletePAPPolicy(String policyId, int tenantId) throws EntitlementException {

        Connection connection = IdentityDatabaseUtil.getDBConnection(true);
        try {
            if (isPolicyPublished(policyId, tenantId)) {
                try (NamedPreparedStatement removePolicyByIdAndVersionPrepStmt = new NamedPreparedStatement(connection,
                        DELETE_UNPUBLISHED_POLICY_VERSIONS_SQL);
                     NamedPreparedStatement removePolicyFromPAPPrepStmt = new NamedPreparedStatement(connection,
                             DELETE_PAP_POLICY_SQL)) {

                    // Remove the unpublished versions of the policy from the database
                    removePolicyByIdAndVersionPrepStmt.setBoolean(IS_IN_PDP, !IN_PDP);
                    removePolicyByIdAndVersionPrepStmt.setString(POLICY_ID, policyId);
                    removePolicyByIdAndVersionPrepStmt.setInt(TENANT_ID, tenantId);
                    removePolicyByIdAndVersionPrepStmt.executeUpdate();

                    // Remove the published version of the policy from the PAP (It is still present in PDP)
                    removePolicyFromPAPPrepStmt.setBoolean(IS_IN_PAP, !IN_PAP);
                    removePolicyFromPAPPrepStmt.setBoolean(IS_IN_PDP, IN_PDP);
                    removePolicyFromPAPPrepStmt.setString(POLICY_ID, policyId);
                    removePolicyFromPAPPrepStmt.setInt(TENANT_ID, tenantId);
                    removePolicyFromPAPPrepStmt.executeUpdate();
                }
            } else {
                try (NamedPreparedStatement removePolicyPrepStmt = new NamedPreparedStatement(connection,
                        DELETE_POLICY_SQL)) {
                    // Remove the policy from the database
                    removePolicyPrepStmt.setString(POLICY_ID, policyId);
                    removePolicyPrepStmt.setInt(TENANT_ID, tenantId);
                    removePolicyPrepStmt.executeUpdate();
                }
            }

            IdentityDatabaseUtil.commitTransaction(connection);

        } catch (SQLException e) {
            IdentityDatabaseUtil.rollbackTransaction(connection);
            throw new EntitlementException(
                    String.format("Error while removing policy %s from PAP policy store", policyId), e);
        } finally {
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }

    /**
     * DAO method to get the published policy from PDP.
     *
     * @param policyId policy ID.
     * @param tenantId tenant ID.
     * @return latest version of the policy.
     */
    public PolicyDTO getPDPPolicy(String policyId, int tenantId) {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            try (NamedPreparedStatement prepStmt = new NamedPreparedStatement(connection, GET_PDP_POLICY_SQL)) {
                prepStmt.setBoolean(IS_IN_PDP, IN_PDP);
                prepStmt.setString(POLICY_ID, policyId);
                prepStmt.setInt(TENANT_ID, tenantId);

                try (ResultSet resultSet = prepStmt.executeQuery()) {
                    if (resultSet.next()) {
                        PolicyDTO dto = new PolicyDTO();
                        String policyString = resultSet.getString(POLICY);
                        dto.setPolicyId(policyId);
                        dto.setPolicy(policyString);
                        dto.setPolicyOrder(resultSet.getInt(POLICY_ORDER));
                        dto.setActive(resultSet.getBoolean(IS_ACTIVE));
                        dto.setPolicyType(resultSet.getString(POLICY_TYPE));
                        // Get policy attributes
                        int version = resultSet.getInt(VERSION);
                        dto.setAttributeDTOs(getPolicyAttributes(connection, tenantId, policyId, version));
                        return dto;
                    }
                }
            }
        } catch (SQLException e) {
            LOG.error(String.format("Error while retrieving PDP policy %s", policyId), e);
        }
        return null;
    }

    /**
     * DAO method to returns all the published policies as PolicyDTOs.
     *
     * @return policies as PolicyDTO[].
     * @throws EntitlementException throws if fails.
     */
    public PolicyDTO[] getAllPDPPolicies(int tenantId) throws EntitlementException {

        List<PolicyDTO> policies = new ArrayList<>();

        LOG.debug("Retrieving all PDP entitlement policies");
        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            try (NamedPreparedStatement prepStmt = new NamedPreparedStatement(connection, GET_ALL_PDP_POLICIES_SQL)) {

                prepStmt.setBoolean(IS_IN_PDP, IN_PDP);
                prepStmt.setInt(TENANT_ID, tenantId);

                try (ResultSet policySet = prepStmt.executeQuery()) {
                    while (policySet.next()) {
                        String policy = policySet.getString(POLICY);
                        AbstractPolicy absPolicy = PAPPolicyReader.getInstance(null).getPolicy(policy);
                        String policyId = absPolicy.getId().toASCIIString();
                        int version = policySet.getInt(VERSION);

                        PolicyDTO dto = new PolicyDTO();
                        dto.setPolicyId(policyId);
                        dto.setPolicy(policy);
                        dto.setPolicyOrder(policySet.getInt(POLICY_ORDER));
                        dto.setActive(policySet.getBoolean(IS_ACTIVE));
                        // Get policy attributes
                        dto.setAttributeDTOs(getPolicyAttributes(connection, tenantId, policyId, version));

                        policies.add(dto);
                    }
                    return policies.toArray(new PolicyDTO[0]);
                }
            }
        } catch (SQLException e) {
            throw new EntitlementException("Error while retrieving PDP policies", e);
        }
    }

    /**
     * DAO method to publish a new policy version.
     *
     * @param policy   policy.
     * @param tenantId tenant ID.
     * @throws EntitlementException If an error occurs.
     */
    public void insertOrUpdatePolicy(PolicyStoreDTO policy, int tenantId) throws EntitlementException {

        Connection connection = IdentityDatabaseUtil.getDBConnection(true);
        try {
            int version = Integer.parseInt(policy.getVersion());
            if (policy.isSetActive()) {
                updateActiveStatus(connection, policy, version, tenantId);
            }
            if (policy.isSetOrder() && policy.getPolicyOrder() > 0) {
                updateOrder(connection, policy, version, tenantId);
            }

            boolean previousActive = false;
            int previousOrder = 0;
            if (!policy.isSetActive() && !policy.isSetOrder()) {
                // Get active status and order of the previously published policy version.
                try (NamedPreparedStatement getActiveStatusAndOrderPrepStmt = new NamedPreparedStatement(connection,
                        GET_ACTIVE_STATUS_AND_ORDER_SQL)) {
                    getActiveStatusAndOrderPrepStmt.setBoolean(IS_IN_PDP, IN_PDP);
                    getActiveStatusAndOrderPrepStmt.setString(POLICY_ID, policy.getPolicyId());
                    getActiveStatusAndOrderPrepStmt.setInt(TENANT_ID, tenantId);
                    try (ResultSet rs = getActiveStatusAndOrderPrepStmt.executeQuery()) {
                        if (rs.next()) {
                            previousActive = rs.getBoolean(IS_ACTIVE);
                            previousOrder = rs.getInt(POLICY_ORDER);
                        }
                    }
                }

                // Remove previously published versions of the policy.
                try (NamedPreparedStatement updatePublishStatusPrepStmt = new NamedPreparedStatement(connection,
                        DELETE_PUBLISHED_VERSIONS_SQL)) {
                    updatePublishStatusPrepStmt.setBoolean(IS_IN_PDP, !IN_PDP);
                    updatePublishStatusPrepStmt.setBoolean(IS_ACTIVE, INACTIVE);
                    updatePublishStatusPrepStmt.setInt(POLICY_ORDER, DEFAULT_POLICY_ORDER);
                    updatePublishStatusPrepStmt.setBoolean(IS_IN_PDP_1, IN_PDP);
                    updatePublishStatusPrepStmt.setString(POLICY_ID, policy.getPolicyId());
                    updatePublishStatusPrepStmt.setInt(TENANT_ID, tenantId);
                    updatePublishStatusPrepStmt.executeUpdate();
                }

                // When removing previously published versions,
                // If the policy has been already removed from PAP, remove the policy from the database.
                try (NamedPreparedStatement removePolicyPrepStmt = new NamedPreparedStatement(connection,
                        DELETE_UNUSED_POLICY_SQL)) {
                    removePolicyPrepStmt.setBoolean(IS_IN_PAP, !IN_PAP);
                    removePolicyPrepStmt.setBoolean(IS_IN_PDP, !IN_PDP);
                    removePolicyPrepStmt.setString(POLICY_ID, policy.getPolicyId());
                    removePolicyPrepStmt.setInt(TENANT_ID, tenantId);
                    removePolicyPrepStmt.executeUpdate();
                }
            }

            // Publish the given version of the policy
            publishPolicyVersion(policy, tenantId, connection, version);

            // If this is not an update, keep the previous active status and order
            if (!policy.isSetActive() && !policy.isSetOrder()) {
                try (NamedPreparedStatement updatePolicyStatusAndOrderPrepStmt = new NamedPreparedStatement(connection,
                        RESTORE_ACTIVE_STATUS_AND_ORDER_SQL)) {
                    updatePolicyStatusAndOrderPrepStmt.setBoolean(IS_ACTIVE, previousActive);
                    updatePolicyStatusAndOrderPrepStmt.setInt(POLICY_ORDER, previousOrder);
                    updatePolicyStatusAndOrderPrepStmt.setString(POLICY_ID, policy.getPolicyId());
                    updatePolicyStatusAndOrderPrepStmt.setInt(VERSION, version);
                    updatePolicyStatusAndOrderPrepStmt.setInt(TENANT_ID, tenantId);
                    updatePolicyStatusAndOrderPrepStmt.executeUpdate();
                }
            }
            IdentityDatabaseUtil.commitTransaction(connection);

        } catch (SQLException e) {
            IdentityDatabaseUtil.rollbackTransaction(connection);
            throw new EntitlementException("Error while publishing policy", e);
        } finally {
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }

    /**
     * DAO method to update the active status or order of a published policy.
     *
     * @param policy   policy.
     * @param tenantId tenant ID.
     * @throws EntitlementException If an error occurs.
     */
    public void updateActiveStatusAndOrder(PolicyStoreDTO policy, int tenantId) throws EntitlementException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            int version = Integer.parseInt(policy.getVersion());
            if (policy.isSetActive()) {
                updateActiveStatus(connection, policy, version, tenantId);
            }
            if (policy.isSetOrder() && policy.getPolicyOrder() > 0) {
                updateOrder(connection, policy, version, tenantId);
            }
        } catch (SQLException | EntitlementException e) {
            throw new EntitlementException(String.format("Error while publishing policy %s", policy.getPolicyId()), e);
        }
    }

    /**
     * DAO method to get the version of a published policy.
     *
     * @param policy   policy.
     * @param tenantId tenant ID.
     * @throws EntitlementException throws, if fails.
     */
    public int getPublishedVersion(PolicyStoreDTO policy, int tenantId) throws EntitlementException {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(true)) {
            try (NamedPreparedStatement getPublishedVersionPrepStmt = new NamedPreparedStatement(connection,
                    GET_PUBLISHED_POLICY_VERSION_SQL)) {
                getPublishedVersionPrepStmt.setBoolean(IS_IN_PDP, IN_PDP);
                getPublishedVersionPrepStmt.setString(POLICY_ID, policy.getPolicyId());
                getPublishedVersionPrepStmt.setInt(TENANT_ID, tenantId);
                try (ResultSet rs = getPublishedVersionPrepStmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(VERSION);
                    }
                }
            }
        } catch (SQLException e) {
            throw new EntitlementException(String.format("Error while getting published version of policy %s",
                    policy.getPolicyId()));
        }
        return -1;
    }

    /**
     * DAO method to unpublish the given policy from PDP.
     *
     * @param policyId policy ID.
     * @param tenantId tenant ID.
     * @return whether the policy version is deleted or not.
     */
    public boolean unpublishPolicy(String policyId, int tenantId) {

        Connection connection = IdentityDatabaseUtil.getDBConnection(true);
        try (NamedPreparedStatement demotePolicyPrepStmt = new NamedPreparedStatement(connection,
                DELETE_PUBLISHED_VERSIONS_SQL);
             NamedPreparedStatement removePolicyPrepStmt = new NamedPreparedStatement(connection,
                     DELETE_UNUSED_POLICY_SQL)) {
            // Remove the published state of the given policy (Remove from PDP)
            demotePolicyPrepStmt.setBoolean(IS_IN_PDP, !IN_PDP);
            demotePolicyPrepStmt.setBoolean(IS_ACTIVE, INACTIVE);
            demotePolicyPrepStmt.setInt(POLICY_ORDER, DEFAULT_POLICY_ORDER);
            demotePolicyPrepStmt.setBoolean(IS_IN_PDP_1, IN_PDP);
            demotePolicyPrepStmt.setString(POLICY_ID, policyId);
            demotePolicyPrepStmt.setInt(TENANT_ID, tenantId);
            demotePolicyPrepStmt.executeUpdate();

            // If the policy has been already removed from PAP, remove the policy from the database
            removePolicyPrepStmt.setBoolean(IS_IN_PAP, !IN_PAP);
            removePolicyPrepStmt.setBoolean(IS_IN_PDP, !IN_PDP);
            removePolicyPrepStmt.setString(POLICY_ID, policyId);
            removePolicyPrepStmt.setInt(TENANT_ID, tenantId);
            removePolicyPrepStmt.executeUpdate();

            IdentityDatabaseUtil.commitTransaction(connection);
            return true;
        } catch (SQLException e) {
            IdentityDatabaseUtil.rollbackTransaction(connection);
            LOG.error(String.format("Error while demoting policy %s", policyId), e);
            return false;
        } finally {
            IdentityDatabaseUtil.closeConnection(connection);
        }
    }

    /**
     * DAO method to check if the policy is published.
     *
     * @param policyId policy ID.
     * @param tenantId tenant ID.
     * @return whether the policy is published or not.
     */
    public boolean isPolicyPublished(String policyId, int tenantId) {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            try (NamedPreparedStatement prepStmt = new NamedPreparedStatement(connection,
                    GET_POLICY_PDP_PRESENCE_SQL)) {
                prepStmt.setBoolean(IS_IN_PDP, IN_PDP);
                prepStmt.setString(POLICY_ID, policyId);
                prepStmt.setInt(TENANT_ID, tenantId);

                try (ResultSet rs = prepStmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            LOG.error(String.format("Error while checking the published status of the policy %s", policyId), e);
            return false;
        }
    }

    /**
     * DAO method to check the existence of the policy in PAP.
     *
     * @param policyId policy ID.
     * @param tenantId tenant ID.
     * @return whether the policy exists in PAP or not.
     */
    public boolean isPAPPolicyExists(String policyId, int tenantId) {

        try (Connection connection = IdentityDatabaseUtil.getDBConnection(false)) {
            try (NamedPreparedStatement getPolicyPublishStatus = new NamedPreparedStatement(connection,
                    GET_POLICY_PAP_PRESENCE_SQL)) {
                getPolicyPublishStatus.setBoolean(IS_IN_PAP, IN_PAP);
                getPolicyPublishStatus.setString(POLICY_ID, policyId);
                getPolicyPublishStatus.setInt(TENANT_ID, tenantId);

                try (ResultSet rs = getPolicyPublishStatus.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            LOG.error(String.format("Error while checking the existence of the policy %s.", policyId), e);
            return false;
        }
    }

    private List<String> getPolicyReferences(Connection connection, int tenantId, String policyId, int version)
            throws SQLException {

        List<String> policyReferences = new ArrayList<>();
        try (NamedPreparedStatement getPolicyRefsPrepStmt = new NamedPreparedStatement(connection,
                GET_PAP_POLICY_REFS_SQL)) {
            getPolicyRefsPrepStmt.setString(POLICY_ID, policyId);
            getPolicyRefsPrepStmt.setInt(VERSION, version);
            getPolicyRefsPrepStmt.setInt(TENANT_ID, tenantId);
            try (ResultSet policyRefs = getPolicyRefsPrepStmt.executeQuery()) {
                while (policyRefs.next()) {
                    policyReferences.add(
                            policyRefs.getString(REFERENCE));
                }
            }
            return policyReferences;
        }
    }

    private List<String> getPolicySetReferences(Connection connection, int tenantId, String policyId, int version)
            throws SQLException {

        List<String> policySetReferences = new ArrayList<>();
        try (NamedPreparedStatement getPolicySetRefsPrepStmt = new NamedPreparedStatement(connection,
                GET_PAP_POLICY_SET_REFS_SQL)) {

            getPolicySetRefsPrepStmt.setString(POLICY_ID, policyId);
            getPolicySetRefsPrepStmt.setInt(VERSION, version);
            getPolicySetRefsPrepStmt.setInt(TENANT_ID, tenantId);
            try (ResultSet policySetRefs = getPolicySetRefsPrepStmt.executeQuery()) {
                while (policySetRefs.next()) {
                    policySetReferences.add(
                            policySetRefs.getString(SET_REFERENCE));
                }
            }
            return policySetReferences;
        }
    }

    private String[] getPolicyEditorData(Connection connection, int tenantId, String policyId, int version)
            throws SQLException {

        try (NamedPreparedStatement getPolicyEditorDataPrepStmt = new NamedPreparedStatement(connection,
                GET_PAP_POLICY_EDITOR_DATA_SQL)) {
            getPolicyEditorDataPrepStmt.setString(POLICY_ID, policyId);
            getPolicyEditorDataPrepStmt.setInt(VERSION, version);
            getPolicyEditorDataPrepStmt.setInt(TENANT_ID, tenantId);

            try (ResultSet editorMetadata = getPolicyEditorDataPrepStmt.executeQuery()) {

                List<String> basicPolicyEditorMetaDataList = new ArrayList<>();
                if (editorMetadata != null) {
                    while (editorMetadata.next()) {
                        int dataOrder = editorMetadata.getInt(EDITOR_DATA_ORDER);
                        while (basicPolicyEditorMetaDataList.size() <= dataOrder) {
                            basicPolicyEditorMetaDataList.add(null);
                        }
                        basicPolicyEditorMetaDataList.set(dataOrder, editorMetadata.getString(EDITOR_DATA));
                    }
                }
                return basicPolicyEditorMetaDataList.toArray(new String[0]);
            }
        }
    }

    private AttributeDTO[] getPolicyAttributes(Connection connection, int tenantId, String policyId, int version)
            throws SQLException {

        List<AttributeDTO> attributeDTOs = new ArrayList<>();
        try (NamedPreparedStatement getPolicyMetaDataPrepStmt =
                     new NamedPreparedStatement(connection, GET_PAP_POLICY_META_DATA_SQL)) {
            getPolicyMetaDataPrepStmt.setString(POLICY_ID, policyId);
            getPolicyMetaDataPrepStmt.setInt(VERSION, version);
            getPolicyMetaDataPrepStmt.setInt(TENANT_ID, tenantId);

            try (ResultSet metadata = getPolicyMetaDataPrepStmt.executeQuery()) {
                while (metadata.next()) {
                    AttributeDTO attributeDTO = new AttributeDTO();
                    attributeDTO.setCategory(metadata.getString(CATEGORY));
                    attributeDTO.setAttributeValue(metadata.getString(ATTRIBUTE_VALUE));
                    attributeDTO.setAttributeId(metadata.getString(ATTRIBUTE_ID));
                    attributeDTO.setAttributeDataType(metadata.getString(DATA_TYPE));
                    attributeDTOs.add(attributeDTO);
                }
            }
        }
        return attributeDTOs.toArray(new AttributeDTO[0]);
    }

    private void insertPolicy(Connection connection, PolicyDTO policy, int tenantId) throws SQLException {

        try (NamedPreparedStatement createPolicyPrepStmt = new NamedPreparedStatement(connection,
                CREATE_PAP_POLICY_SQL)) {

            createPolicyPrepStmt.setString(POLICY_ID, policy.getPolicyId());
            createPolicyPrepStmt.setInt(VERSION, Integer.parseInt(policy.getVersion()));
            createPolicyPrepStmt.setBoolean(IS_IN_PDP, policy.isPromote());
            createPolicyPrepStmt.setBoolean(IS_IN_PAP, IN_PAP);
            createPolicyPrepStmt.setString(POLICY, policy.getPolicy());
            createPolicyPrepStmt.setBoolean(IS_ACTIVE, policy.isActive());
            createPolicyPrepStmt.setString(POLICY_TYPE, policy.getPolicyType());
            createPolicyPrepStmt.setString(POLICY_EDITOR, policy.getPolicyEditor());
            createPolicyPrepStmt.setInt(POLICY_ORDER, DEFAULT_POLICY_ORDER);
            createPolicyPrepStmt.setTimeStamp(LAST_MODIFIED_TIME, new Timestamp(System.currentTimeMillis()),
                    Calendar.getInstance(TimeZone.getTimeZone(UTC)));
            createPolicyPrepStmt.setString(LAST_MODIFIED_USER,
                    CarbonContext.getThreadLocalCarbonContext().getUsername());
            createPolicyPrepStmt.setInt(TENANT_ID, tenantId);

            createPolicyPrepStmt.executeUpdate();
        }
    }

    private void insertPolicyReferences(Connection connection, PolicyDTO policy, int tenantId)
            throws SQLException {

        String[] policyIdReferences = policy.getPolicyIdReferences();
        String[] policySetIdReferences = policy.getPolicySetIdReferences();

        try (NamedPreparedStatement createPolicyReferencesPrepStmt = new NamedPreparedStatement(connection,
                CREATE_PAP_POLICY_REFS_SQL);
             NamedPreparedStatement createPolicySetReferencesPrepStmt = new NamedPreparedStatement(connection,
                     CREATE_PAP_POLICY_SET_REFS_SQL)) {

            for (String policyIdReference : policyIdReferences) {
                createPolicyReferencesPrepStmt.setString(REFERENCE, policyIdReference);
                createPolicyReferencesPrepStmt.setString(POLICY_ID, policy.getPolicyId());
                createPolicyReferencesPrepStmt.setInt(VERSION, Integer.parseInt(policy.getVersion()));
                createPolicyReferencesPrepStmt.setInt(TENANT_ID, tenantId);
                createPolicyReferencesPrepStmt.addBatch();
            }
            createPolicyReferencesPrepStmt.executeBatch();

            for (String policySetReference : policySetIdReferences) {
                createPolicySetReferencesPrepStmt.setString(SET_REFERENCE, policySetReference);
                createPolicySetReferencesPrepStmt.setString(POLICY_ID, policy.getPolicyId());
                createPolicySetReferencesPrepStmt.setInt(VERSION, Integer.parseInt(policy.getVersion()));
                createPolicySetReferencesPrepStmt.setInt(TENANT_ID, tenantId);
                createPolicySetReferencesPrepStmt.addBatch();
            }
            createPolicySetReferencesPrepStmt.executeBatch();
        }
    }

    private void insertPolicyAttributes(Connection connection, PolicyDTO policy, int tenantId) throws SQLException {

        try (NamedPreparedStatement createAttributesPrepStmt = new NamedPreparedStatement(connection,
                CREATE_PAP_POLICY_ATTRIBUTES_SQL)) {

            AttributeDTO[] attributeDTOs = policy.getAttributeDTOs();
            for (AttributeDTO attributeDTO : attributeDTOs) {

                createAttributesPrepStmt.setString(ATTRIBUTE_ID, attributeDTO.getAttributeId());
                createAttributesPrepStmt.setString(ATTRIBUTE_VALUE, attributeDTO.getAttributeValue());
                createAttributesPrepStmt.setString(DATA_TYPE, attributeDTO.getAttributeDataType());
                createAttributesPrepStmt.setString(CATEGORY, attributeDTO.getCategory());
                createAttributesPrepStmt.setString(POLICY_ID, policy.getPolicyId());
                createAttributesPrepStmt.setInt(VERSION, Integer.parseInt(policy.getVersion()));
                createAttributesPrepStmt.setInt(TENANT_ID, tenantId);
                createAttributesPrepStmt.addBatch();
            }
            createAttributesPrepStmt.executeBatch();
        }
    }

    private void insertPolicyEditorData(Connection connection, PolicyDTO policy, int tenantId) throws SQLException {

        // Find policy meta data
        String[] policyMetaData = policy.getPolicyEditorData();
        if (policyMetaData != null && policyMetaData.length > 0) {
            try (NamedPreparedStatement createPolicyEditorDataPrepStmt = new NamedPreparedStatement(connection,
                    CREATE_PAP_POLICY_EDITOR_DATA_SQL)) {
                int index = 0;
                for (String policyData : policyMetaData) {
                    createPolicyEditorDataPrepStmt.setInt(EDITOR_DATA_ORDER, index);
                    createPolicyEditorDataPrepStmt.setString(EDITOR_DATA, policyData);
                    createPolicyEditorDataPrepStmt.setString(POLICY_ID, policy.getPolicyId());
                    createPolicyEditorDataPrepStmt.setInt(VERSION, Integer.parseInt(policy.getVersion()));
                    createPolicyEditorDataPrepStmt.setInt(TENANT_ID, tenantId);

                    createPolicyEditorDataPrepStmt.addBatch();
                    index++;
                }
                createPolicyEditorDataPrepStmt.executeBatch();
            }
        }
    }

    private void updateOrder(Connection connection, PolicyStoreDTO policy, int version, int tenantId)
            throws EntitlementException {

        try (NamedPreparedStatement updateOrderPrepStmt = new NamedPreparedStatement(connection,
                UPDATE_ORDER_SQL)) {
            int order = policy.getPolicyOrder();
            updateOrderPrepStmt.setInt(POLICY_ORDER, order);
            updateOrderPrepStmt.setString(POLICY_ID, policy.getPolicyId());
            updateOrderPrepStmt.setInt(VERSION, version);
            updateOrderPrepStmt.setInt(TENANT_ID, tenantId);
            updateOrderPrepStmt.executeUpdate();
            IdentityDatabaseUtil.closeStatement(updateOrderPrepStmt);
        } catch (SQLException e) {
            throw new EntitlementException(
                    String.format("Error while updating policy order of policy %s", policy.getPolicyId()), e);
        }
    }

    private void updateActiveStatus(Connection connection, PolicyStoreDTO policy, int version, int tenantId)
            throws EntitlementException {

        try (NamedPreparedStatement updateActiveStatusPrepStmt = new NamedPreparedStatement(connection,
                UPDATE_ACTIVE_STATUS_SQL)) {
            updateActiveStatusPrepStmt.setBoolean(IS_ACTIVE, policy.isActive());
            updateActiveStatusPrepStmt.setString(POLICY_ID, policy.getPolicyId());
            updateActiveStatusPrepStmt.setInt(VERSION, version);
            updateActiveStatusPrepStmt.setInt(TENANT_ID, tenantId);
            updateActiveStatusPrepStmt.executeUpdate();
            IdentityDatabaseUtil.closeStatement(updateActiveStatusPrepStmt);
        } catch (SQLException e) {
            throw new EntitlementException(
                    String.format("Error while enabling or disabling policy %s", policy.getPolicyId()), e);
        }
    }

    private void publishPolicyVersion(PolicyStoreDTO policy, int tenantId, Connection connection, int version)
            throws SQLException {

        try (NamedPreparedStatement publishPolicyPrepStmt = new NamedPreparedStatement(connection,
                PUBLISH_POLICY_VERSION_SQL)) {
            publishPolicyPrepStmt.setBoolean(IS_IN_PDP, IN_PDP);
            publishPolicyPrepStmt.setString(POLICY_ID, policy.getPolicyId());
            publishPolicyPrepStmt.setInt(VERSION, version);
            publishPolicyPrepStmt.setInt(TENANT_ID, tenantId);
            publishPolicyPrepStmt.executeUpdate();
        }
    }

    /**
     * Returns given policy version as a PolicyDTO.
     *
     * @param policy policy.
     * @return policy as a PolicyDTO.
     * @throws SQLException throws, if fails.
     */
    private PolicyDTO getPolicyDTO(ResultSet policy, Connection connection) throws SQLException {

        String policyId = policy.getString(POLICY_ID);
        String version = String.valueOf(policy.getInt(VERSION));
        int tenantId = policy.getInt(TENANT_ID);

        PolicyDTO dto = new PolicyDTO();
        dto.setPolicyId(policyId);
        dto.setVersion(version);
        dto.setLastModifiedTime(String.valueOf(policy.getTimestamp(LAST_MODIFIED_TIME).getTime()));
        dto.setLastModifiedUser(policy.getString(LAST_MODIFIED_USER));
        dto.setActive(policy.getBoolean(IS_ACTIVE));
        dto.setPolicyOrder(policy.getInt(POLICY_ORDER));
        dto.setPolicyType(policy.getString(POLICY_TYPE));
        dto.setPolicyEditor(policy.getString(POLICY_EDITOR));
        dto.setPolicy(policy.getString(POLICY));

        // Get policy references
        List<String> policyReferences = getPolicyReferences(connection, tenantId, policyId, Integer.parseInt(version));
        dto.setPolicyIdReferences(policyReferences.toArray(new String[0]));

        // Get policy set references
        List<String> policySetReferences =
                getPolicySetReferences(connection, tenantId, policyId, Integer.parseInt(version));
        dto.setPolicySetIdReferences(policySetReferences.toArray(new String[0]));

        // Get policy editor data
        String[] basicPolicyEditorMetaData =
                getPolicyEditorData(connection, tenantId, policyId, Integer.parseInt(version));
        dto.setPolicyEditorData(basicPolicyEditorMetaData);

        // Get policy metadata
        AttributeDTO[] attributeDTOs = getPolicyAttributes(connection, tenantId, policyId, Integer.parseInt(version));
        dto.setAttributeDTOs(attributeDTOs);

        return dto;
    }
}
