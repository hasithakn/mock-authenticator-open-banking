/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.com). All Rights Reserved.
 *
 * This software is the property of WSO2 Inc. and its suppliers, if any.
 * Dissemination of any information or reproduction of any material contained
 * herein is strictly forbidden, unless permitted by WSO2 in accordance with
 * the WSO2 Commercial License available at http://wso2.com/licenses. For specific
 * language governing the permissions and limitations under this license,
 * please see the license as well as any agreement you’ve entered into with
 * WSO2 governing the purchase of this software and any associated services.
 */

function approvedDefaultClaim() {
    var mandatoryClaimCBs = $(".mandatory-claim");
    var checkedMandatoryClaimCBs = $(".mandatory-claim:checked");
    var scopeApproval = $("input[name='scope-approval']");

    // If scope approval radio button is rendered then we need to validate that it's checked
    if (scopeApproval.length > 0) {
        if (scopeApproval.is(":checked")) {
            var checkScopeConsent = $("input[name='scope-approval']:checked");
            $('#consent').val(checkScopeConsent.val());
        } else {
            $("#modal_scope_validation").modal();
            return;
        }
    } else {
        // Scope radio button was not rendered therefore set the consent to 'approve'
        document.getElementById('consent').value = "approve";
    }

    if (checkedMandatoryClaimCBs.length === mandatoryClaimCBs.length) {
        document.getElementById("profile").submit();
    } else {
        $("#modal_claim_validation").modal();
    }
}

function denyDefaultClaim() {
    document.getElementById('consent').value = "deny";
    document.getElementById("profile").submit();
}

// Update the selected account list according to the selected checkbox values.
function updateAcc() {
    var accIds = "";
    var accNames = "";
    //Get values from checked checkboxes
    $("input:checkbox[name=chkAccounts]:checked").each(function(){
        accIds =  accIds.concat(":", $(this).val());
        accNames =  accNames.concat(":", $(this).attr("id"));
    });
    accIds = accIds.replace(/^\:/, '');
    accNames = accNames.replace(/^\:/, '');
    document.getElementById('account').value = accIds;
    document.getElementById('accountName').value = accNames;
}

// Confirm selected accounts
function approvedAcc() {
    updateAcc();
    document.getElementById('consent').value = "approve";
    validateAccFrm();
}

// Submit account selection from
function validateAccFrm() {
    if (document.getElementById('type').value == "accounts") {
        if (document.getElementById('account').value === "" ||
            document.getElementById('account').value === "default") {
            $(".acc-err").show();
            return false;
        } else {
            document.getElementById("oauth2_authz_consent").submit();
        }
    }

    if (document.getElementById('type').value == "payments") {
        if (document.getElementById('paymentAccount').value === "" ||
            document.getElementById('paymentAccount').value === "default") {
            $(".acc-err").show();
            return false;
        } else {
            document.getElementById("oauth2_authz_confirm").submit();
        }
    }

    if (document.getElementById('type').value == "fundsconfirmations") {
        document.getElementById("oauth2_authz_confirm").submit();
    }
}

// Confirm sharing data
function approvedAU() {
    document.getElementById('consent').value = true;
    validateAUFrm();
}

function deny() {
    document.getElementById('consent').value = false;
    document.getElementById("oauth2_authz_confirm").submit();
}

// Submit data sharing from
function validateAUFrm() {
    if (document.getElementById('type').value == "accounts") {
        if (document.getElementById('account').value === "" ||
            document.getElementById('account').value === "default") {
            $(".acc-err").show();
            return false;
        } else {
            console.log("Hello");
            setTimeout(() => {  console.log("World!"); }, 2000);
            document.getElementById("oauth2_authz_confirm").submit();
        }
    }

    if (document.getElementById('type').value == "payments") {
        if (document.getElementById('paymentAccount').value === "" ||
            document.getElementById('paymentAccount').value === "default") {
            $(".acc-err").show();
            return false;
        } else {
            document.getElementById("oauth2_authz_confirm").submit();
        }
    }

    if (document.getElementById('type').value == "fundsconfirmations") {
        document.getElementById("oauth2_authz_confirm").submit();
    }
}