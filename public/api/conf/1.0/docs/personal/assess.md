# Bank Account Reputation Microservice - Personal Assess Endpoint

## Overview

### What and how?
This endpoint checks the _likely_ correctness of a given personal bank account and it's _likely_ connection to the given account holder (aka the _subject_).

The following checks are performed:
1. The format and content of the sort code and account number, via the [BACS standard modulus check](https://www.vocalink.com/tools/modulus-checking/). (This is an internal check, so highly resilient).
1. The existence of the account within a third party personal bank account dataset. (This calls off to an external service, so may fail).
1. If the third-party _does_ locate the account, then the similarity of the account holder details it holds, compared with the provided subject details. (This calls off to an external service, so may fail).
  
**Note**: If calls to the third party service fail, the results of earlier checks are still returned. 

### How to use the results
This service has two benefits:
* It can increase the likelihood of correct bank details being captured, by having the user check and correct details that _may_ be incorrect, in the event of an error.
* It can indicate if the transaction might be fraudulent (e.g. the account holder details held against the matched bank details don't match those provided by the user).

It's important to note that the sort code database and third party service have *incomplete* coverage. Because of this, for user-facing services, negative results from related checks do *not* mean the provided bank/subject details are *necessarily* incorrect, but rather that the user should check and correct provided details before retrying. It may be appropriate to allow the user journey to proceed along a happy-path, even if the third party service checks remain negative. 

For more details see [Interpreting the response](#interpreting-the-response).

## Usage
### URL and Method
URL: `/verify/personal`

Method: `POST`

### Request message body
A JSON message equivalent to the following Scala `Input` case class and its associates:

```scala
case class Account(
                    sortCode: String, // The bank sort code, 6 characters long (whitespace and/or dashes should be removed)
                    accountNumber: String // The bank account number, 8 characters long
                  )

case class Address(
                    lines: List[String], // One to four lines; cumulative length must be between 1 and 140 characters.
                    town: Option[String], // Must be between 1 and 35 characters long
                    postcode: Option[String] // Must be between 5 and 8 characters long, all uppercase. The internal space character can be omitted.
                  )

case class Subject(
                    title: Option[String], // e.g. "Mr" etc; must >= 2 character and <= 35 characters long
                    name: Option[String], // Must be between 1 and 70 characters long
                    firstName: Option[String], // Must be between 1 and 35 characters long
                    lastName: Option[String], // Must be between 1 and 35 characters long
                    dob: Option[String], // date of birth: ISO-8601 YYYY-MM-DD
                    address: Option[Address]
                  ) {

  require(
    (name.isEmpty && firstName.isDefined && lastName.isDefined) ||
    (name.isDefined && firstName.isEmpty && lastName.isEmpty)
  )
}

case class Input(
                  account: Account,
                  subject: Subject
                )
```

The `Input` object is equivalent to the JSON schema in [personal-request.json](personal-request.json).

**Note** that use of the separate `firstName` and `lastName` fields is preferable over `name`, as this can give better matching success. Our service will separate `name` into two fields before calling the third party API, whereas `firstName` and `lastName` are sent as-is.

An example of `Input` is in [input-sample1.json](input-sample1.json)

### Headers
* `User-Agent` - required; any string identifying the calling client (typically quite short)
* `X-Tracking-Id` - optional; any string identifying the current session that will be inserted into audit trails.
* `X-Request-ID` - required; a value representing the request, supplied by default when using http-verbs
* `X-Session-ID` - required (for web); a value representing the session, supplied by default when using http-verbs
* `True-Client-IP` - required; the IP address of the external user, supplied by default when using http-verbs
* `True-Client-Port` - required; the port of the external user, supplied by default when using http-verbs

### Response status codes
* **200 OK** given a valid request and the reputation assessment was successfully obtained.
* Others as per [General Error Codes](../../../../../../docs/README.md).

### Response body
* `Content-Type: application/json`

## Reputation Assessment Message Format

The response is a JSON message equivalent to the following Scala:

```
case class Assessment(
    accountNumberIsWellFormatted: String,
    accountExists: String,
    nameMatches: String,
    accountName: Option[String],
    nonStandardAccountDetailsRequiredForBacs: String,
    sortCodeIsPresentOnEISCD: String,
    sortCodeSupportsDirectDebit: String,
    sortCodeSupportsDirectCredit: String,
    sortCodeBankName: Option[String],
    iban: Option[String]
)
```
The `Assessment` object is equivalent to the JSON schema in [personal-response.json](personal-response.json).

Some examples of `Assessment` are in [output-sample1.json](output-sample1.json),
[output-sample2.json](output-sample2.json), [output-sample3.json](output-sample3.json).


## Interpreting the response
### `accountNumberIsWellFormatted`
| Response | Interpretation | Handling within user journey |
| ----------- | ----------- | ----------- |
| `yes` | The sort code is known and the sort code and account number are correctly formatted and their contents pass our implementation of the [BACS standard modulus check](https://www.vocalink.com/tools/modulus-checking/). They may still not resolve to an addressable (for electronic transfer) bank account though. | In the absence of other checks, the user journey should proceed to the next stage. |
| `no` | The sort code is known but the account number and sort code combination did not pass out implementation of the [BACS standard modulus check](https://www.vocalink.com/tools/modulus-checking/). | The user should be given the opportunity to try again, but not proceed if the error continues. |
| `indeterminate` | The sort code is unknown and would not resolve to an addressable bank account, therefore it cannot be mod checked. | This should be treated as a neutral response; defer to other factors such as accountExists to make a decision. |

### `accountExists`
| Response | Interpretation | Handling within user journey |
| ----------- | ----------- | ----------- |
| `yes` | The sort code and account number relate to a bank account stored in the third party service. | In the absence of other checks, the user journey should proceed to the next stage. |
| `no` | The third party service asserted that the account number was not valid for the given sort code. | The user should be given the opportunity to try again, but not proceed if the error continues. |
| `inapplicable` | The sort code and account number failed the modulus check, and so the lookup was not performed. | The user should be given the opportunity to try again, but not proceed if the error continues. |
| `indeterminate` | The sort code and account number were not found in the third party service. This could be because:<ul><li>One of them was mistyped, or completely wrong.</li><li>The bank account and sort code are valid but are missing from the service.</li></ul> | It’s not possible to distinguish between these two reasons. The user should be given the opportunity to try again, and then proceed along the happy path should the error continue. |
| `error` | There was an error calling the third party service. | Either retry (system or user initiated), or proceed to the next check/stage of user journey. |

### `nameMatches`
| Response | Interpretation | Handling within user journey |
| ----------- | ----------- | ----------- |
| `yes` | The provided subject name was found to match the account holder name stored by the third party service. | In the absence of other checks, the user journey should proceed to the next stage. |
| `no` | No match was found for the provided subject name in the third party service. | This could be because it was mistyped, or is completely wrong. The user should be given the opportunity to try again, and then proceed along the happy path should the error continue. |
| `partial` | After normalisation, the provided subject name was found to be a close match to the one stored by the third party service E.g 'Mr P Smith' vs 'Mr Peter Smith'. | This should almost always be treated as a `yes`, but in certain edge cases you may want to give the user the opportunity to try again. When this occurs, the name held by the third party is returned in the `accountName` field for your service to perform further checks if required. |
| `inapplicable` | The sort code and/or account number failed initial validation and no further checks were made | The user should be given the opportunity to try again, but not proceed if the error continues. |
| `indeterminate` | The sort code and account number did not resolve to an account stored in the third party service. | See row for `accountExists`: `indeterminate`. |
| `error` | There was an error calling the third party service. | Either retry (system or user initiated), or proceed to the next check/stage of user journey. |                                         |

### `accountName`
| Response | Interpretation | Handling within user journey |
| ----------- | ----------- | ----------- |
| E.g. `Mr Peter Smith` | In the case of a partial name match, this will be the name on the account as reported by the third party. | Depending on your use case, you can compare this to the input provided by the user to provide extra certainty about your decision to proceed with the user journey. Most services will not need to use this information. |
| `<not present>` | The result for nameMatches was not a partial match |

### `nonStandardAccountDetailsRequiredForBacs`
| Response | Interpretation | Handling within user journey |
| ----------- | ----------- | ----------- |
| `yes` | Indicates that a BACS transaction for this account will require additional information (e.g. a Building Society roll number). This applies to a minority of accounts. | The additional information can not be verified in any way, so when yes is returned for this field, the calling service should ensure the user provides a non-empty additional information parameter as part of the BACS transaction. |
| `no` | See above. | No action required. |
| `inapplicable` | This value will be returned when `accountNumberIsWellFormatted` is `false`. | No action required. |

### `sortCodeIsPresentOnEISCD`
| Response | Interpretation | Handling within user journey |
| ----------- | ----------- | ----------- |
| `yes` | The sort code given was found in the EISCD database | There is a high certainty that the user has entered their sort code correctly |
| `no` | The sort code given was NOT found in the EISCD database | The user may have entered their sort code incorrectly so they should be given a chance to retry |
| `error` | A transient error occurred when accessing the EISCD data held locally to the service | Either an automatic retry, or the user should be given |

### `sortCodeSupportsDirectDebit`
| Response | Interpretation | Handling within user journey |
|:---------|:---------------|:-----------------------------|
| `yes` | The provided sortcode supports direct debit payments and setup. | |
| `no` | The provided sortcode does not support direct debit payments and/or setup. | |
| `error` | There was an error checking the eiscd. | |

### `sortCodeSupportsDirectCredit`
| Response | Interpretation | Handling within user journey |
|:---------|:---------------|:-----------------------------|
| `yes` | The provided sortcode supports direct credit payments. | |
| `no` | The provided sortcode does not support direct credit payments. | |
| `error` | There was an error checking the third party service. | |

### `sortCodeBankName`
| Response | Interpretation | Handling within user journey |
| ----------- | ----------- | ----------- |
| E.g. `LLOYDS BANK PLC`, `MONZO BANK LIMITED` | The EISCD lookup resolved to a valid sort code and the bank name is provided | The bank name can be shown to the user if one of the other checks fails, in order to help them understand why their details were rejected. All bank names returned from the EISCD are capitalised, as per the examples. |
| `<not present>` | The EIDCD lookup was not successful or failed due to an error, no bank name is available | As per response to other fields |

### `iban`
| Response        | Interpretation                                                                                                                        | Handling within user journey |
|-----------------|---------------------------------------------------------------------------------------------------------------------------------------| ----------- |
| A valid IBAN    | Both modcheck and EISCD validation were successful and a IBAN for the UK bank account is generated                                    |  |
| `<not present>` | An IBAN could not be generated: EISCD validation passed but `bicBankCode` wasn't defined, modcheck failed, or EISCD validation failed |  |

### Upstream failures
In the event of the third party service call failing, the resulting response message will be as per rows 10 or 11 in the table below.

## General Error Conditions

See [README](../../../../../../docs/README.md)
