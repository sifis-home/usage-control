import json
import websocket

session_id = "None"
def on_message(ws, message):
    if "ucs-command" in message:
        print("\nReceived message from the ucs:")
        parsed = json.loads(message)
        print(json.dumps(parsed, indent=2))
        print("\n--------------------------------\n")

        if parsed["Volatile"]["value"]["command"]["value"]["message"]["purpose"] == "TRY_RESPONSE":
            global session_id
            session_id = parsed["Volatile"]["value"]["command"]["value"]["message"]["session_id"]

        enter_command()

def on_error(ws, error):
    print(error)


def on_close(ws, close_status_code, close_msg):
    print("### Connection closed ###")


def on_open(ws):
    print("### Connection established ###")
    enter_command()

def enter_command():
    print("")
    print("LIST OF COMMANDS:")
    print("")
    print("  PEP commands:              PAP commands:")
    print("    1 : register               5 : add policy")
    print("    2 : try access             6 : list policies")
    print("    3 : start access           7 : get policy")
    print("    4 : end access             8 : delete policy")
    print("")

    command = input("Enter command number> ")
    print("")
    send_command(command)

def send_command(command):
    if command == "1":
        register()
    elif command == "2":
        try_access()
    elif command == "3":
        start_access()
    elif command == "4":
        end_access()
    elif command == "5":
        add_policy()
    elif command == "6":
        list_policies()
    elif command == "7":
        get_policy()
    elif command == "8":
        delete_policy()
    elif command == "9":
        unrecognized_command()
    else:
        print("command not found")
        enter_command()


def print_and_send(json_out):
    print("Message sent:")
    print(json.dumps(json_out, indent=2))
    ws.send(json.dumps(json_out))

## REGISTER
def register():
    ws_req = {
     "RequestPubMessage": {
       "value": {
         "timestamp": 1684851919378,
         "command": {
           "command_type": "pep-command",
           "value": {
             "message": {
               "purpose": "REGISTER",
               "message_id": "3d0ee6bb-6ec7-403c-aaa1-c27d3e8795a1",
               "sub_topic_name": "topic-name-the-pep-is-subscribed-to",
               "sub_topic_uuid": "topic-uuid-the-pep-is-subscribed-to"
             },
             "id": "pep-websocket_client",
             "topic_name": "topic-name",
             "topic_uuid": "topic-uuid-the-ucs-is-subscribed-to"
           }
         }
       }
     }
    }
    print("\n---------- REGISTER ------------\n")
    print_and_send(ws_req)

## TRY ACCESS
def try_access():
    ws_req = {
        "RequestPubMessage": {
            "value": {
                "timestamp": 1684851920422,
                "command": {
                    "command_type": "pep-command",
                    "value": {
                        "message": {
                            "purpose": "TRY",
                            "message_id": "2b21f0e1-9d2a-4760-8ddb-53ff1af46d7f",
                            "request": "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9InllcyI/Pgo8UmVxdWVzdCBSZXR1cm5Qb2xpY3lJZExpc3Q9ImZhbHNlIiBDb21iaW5lZERlY2lzaW9uPSJmYWxzZSIKICAgICAgICAgeG1sbnM9InVybjpvYXNpczpuYW1lczp0Yzp4YWNtbDozLjA6Y29yZTpzY2hlbWE6d2QtMTciPgogICAgPEF0dHJpYnV0ZXMgQ2F0ZWdvcnk9InVybjpvYXNpczpuYW1lczp0Yzp4YWNtbDoxLjA6c3ViamVjdC1jYXRlZ29yeTphY2Nlc3Mtc3ViamVjdCI+CiAgICAgICAgPEF0dHJpYnV0ZSBBdHRyaWJ1dGVJZD0idXJuOm9hc2lzOm5hbWVzOnRjOnhhY21sOjEuMDpzdWJqZWN0OnN1YmplY3QtaWQiIEluY2x1ZGVJblJlc3VsdD0iZmFsc2UiPgogICAgICAgICAgICA8QXR0cmlidXRlVmFsdWUgRGF0YVR5cGU9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hI3N0cmluZyI+QzwvQXR0cmlidXRlVmFsdWU+CiAgICAgICAgPC9BdHRyaWJ1dGU+CiAgICA8L0F0dHJpYnV0ZXM+CiAgICA8QXR0cmlidXRlcyBDYXRlZ29yeT0idXJuOm9hc2lzOm5hbWVzOnRjOnhhY21sOjMuMDphdHRyaWJ1dGUtY2F0ZWdvcnk6cmVzb3VyY2UiPgogICAgICAgIDxBdHRyaWJ1dGUgQXR0cmlidXRlSWQ9InVybjpvYXNpczpuYW1lczp0Yzp4YWNtbDoxLjA6cmVzb3VyY2U6cmVzb3VyY2UtaWQiIEluY2x1ZGVJblJlc3VsdD0iZmFsc2UiPgogICAgICAgICAgICA8QXR0cmlidXRlVmFsdWUgRGF0YVR5cGU9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hI3N0cmluZyI+UkVTPC9BdHRyaWJ1dGVWYWx1ZT4KICAgICAgICA8L0F0dHJpYnV0ZT4KICAgIDwvQXR0cmlidXRlcz4KICAgIDxBdHRyaWJ1dGVzIENhdGVnb3J5PSJ1cm46b2FzaXM6bmFtZXM6dGM6eGFjbWw6My4wOmF0dHJpYnV0ZS1jYXRlZ29yeTphY3Rpb24iPgogICAgICAgIDxBdHRyaWJ1dGUgQXR0cmlidXRlSWQ9InVybjpvYXNpczpuYW1lczp0Yzp4YWNtbDoxLjA6YWN0aW9uOmFjdGlvbi1pZCIgSW5jbHVkZUluUmVzdWx0PSJmYWxzZSI+CiAgICAgICAgICAgIDxBdHRyaWJ1dGVWYWx1ZSBEYXRhVHlwZT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS9YTUxTY2hlbWEjc3RyaW5nIj5PUDwvQXR0cmlidXRlVmFsdWU+CiAgICAgICAgPC9BdHRyaWJ1dGU+CiAgICA8L0F0dHJpYnV0ZXM+CjwvUmVxdWVzdD4=",
                            "policy": None
                        },
                        "id": "pep-websocket_client",
                        "topic_name": "topic-name",
                        "topic_uuid": "topic-uuid-the-ucs-is-subscribed-to"
                    }
                }
            }
        }
    }
    print("\n--------- TRY ACCESS -----------\n")
    print_and_send(ws_req)

## START ACCESS
def start_access():
    global session_id
    ws_req = {
        "RequestPubMessage": {
            "value": {
                "timestamp": 1684851921396,
                "command": {
                    "command_type": "pep-command",
                    "value": {
                        "message": {
                            "purpose": "START",
                            "message_id": "3985a09e-7679-48c4-bad8-09cd89987bb6",
                            "session_id": session_id
                        },
                        "id": "pep-websocket_client",
                        "topic_name": "topic-name",
                        "topic_uuid": "topic-uuid-the-ucs-is-subscribed-to"
                    }
                }
            }
        }
    }
    print("\n-------- START ACCESS ----------\n")
    check_session_before_sending(ws_req)

## END ACCESS
def end_access():
    global session_id
    ws_req = {
        "RequestPubMessage": {
            "value": {
                "timestamp": 1684851924867,
                "command": {
                    "command_type": "pep-command",
                    "value": {
                        "message": {
                            "purpose": "END",
                            "message_id": "c8860845-d190-4e05-9b9c-6ac689582fdd",
                            "session_id": session_id
                        },
                        "id": "pep-websocket_client",
                        "topic_name": "topic-name",
                        "topic_uuid": "topic-uuid-the-ucs-is-subscribed-to"
                    }
                }
            }
        }
    }
    print("\n--------- END ACCESS -----------\n")
    check_session_before_sending(ws_req)
    session_id = "None"

def check_session_before_sending(ws_req):
    global session_id

    if session_id == "None":
        print("WARNING: No active session yet")
        answer = "?"
        while answer != "y" and answer != "n":
            answer = input("Do you want to proceed anyway? (y/n)> ")
            if answer == "y":
                print_and_send(ws_req)
            elif answer == "n":
                print("\n--------------------------------\n")
                enter_command()
    else:
        print_and_send(ws_req)

## ADD POLICY
def add_policy():
    ws_req = {
        "RequestPubMessage": {
            "value": {
                "timestamp": 1684256524618,
                "command": {
                    "command_type": "pap-command",
                    "value": {
                        "message": {
                            "purpose": "ADD_POLICY",
                            "message_id": "random456-msg_id",
                            "policy": "PFBvbGljeQoJCXhtbG5zPSJ1cm46b2FzaXM6bmFtZXM6dGM6eGFjbWw6My4wOmNvcmU6c2NoZW1hOndkLTE3IgoJCVBvbGljeUlkPSJwb2xpY3lfdGVtcGxhdGUyIgoJCVJ1bGVDb21iaW5pbmdBbGdJZD0idXJuOm9hc2lzOm5hbWVzOnRjOnhhY21sOjMuMDpydWxlLWNvbWJpbmluZy1hbGdvcml0aG06ZGVueS11bmxlc3MtcGVybWl0IgoJCVZlcnNpb249IjMuMCI+Cgk8RGVzY3JpcHRpb24+UG9saWN5PC9EZXNjcmlwdGlvbj4KCTxUYXJnZXQgPgoJCTxBbnlPZiA+CgkJCTxBbGxPZiA+CgkJCQk8TWF0Y2ggTWF0Y2hJZD0idXJuOm9hc2lzOm5hbWVzOnRjOnhhY21sOjEuMDpmdW5jdGlvbjpzdHJpbmctZXF1YWwiID4KCQkJCQk8QXR0cmlidXRlRGVzaWduYXRvcgoJCQkJCQkJQXR0cmlidXRlSWQ9InVybjpvYXNpczpuYW1lczp0Yzp4YWNtbDoxLjA6c3ViamVjdDpzdWJqZWN0LWlkIgoJCQkJCQkJQ2F0ZWdvcnk9InVybjpvYXNpczpuYW1lczp0Yzp4YWNtbDoxLjA6c3ViamVjdC1jYXRlZ29yeTphY2Nlc3Mtc3ViamVjdCIKCQkJCQkJCURhdGFUeXBlPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYSNzdHJpbmciCgkJCQkJCQlNdXN0QmVQcmVzZW50PSJ0cnVlIj4KCQkJCQk8L0F0dHJpYnV0ZURlc2lnbmF0b3I+CgkJCQkJPEF0dHJpYnV0ZVZhbHVlCgkJCQkJCQlEYXRhVHlwZT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS9YTUxTY2hlbWEjc3RyaW5nIj5DPC9BdHRyaWJ1dGVWYWx1ZT4KCQkJCTwvTWF0Y2g+CgkJCQk8TWF0Y2ggTWF0Y2hJZD0idXJuOm9hc2lzOm5hbWVzOnRjOnhhY21sOjEuMDpmdW5jdGlvbjpzdHJpbmctZXF1YWwiID4KCQkJCQk8QXR0cmlidXRlRGVzaWduYXRvcgoJCQkJCQkJQXR0cmlidXRlSWQ9InVybjpvYXNpczpuYW1lczp0Yzp4YWNtbDoxLjA6cmVzb3VyY2U6cmVzb3VyY2Utc2VydmVyIgoJCQkJCQkJQ2F0ZWdvcnk9InVybjpvYXNpczpuYW1lczp0Yzp4YWNtbDozLjA6YXR0cmlidXRlLWNhdGVnb3J5OnJlc291cmNlIgoJCQkJCQkJRGF0YVR5cGU9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hI3N0cmluZyIKCQkJCQkJCU11c3RCZVByZXNlbnQ9InRydWUiPgoJCQkJCTwvQXR0cmlidXRlRGVzaWduYXRvcj4KCQkJCTxBdHRyaWJ1dGVWYWx1ZQoJCQkJCQlEYXRhVHlwZT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS9YTUxTY2hlbWEjc3RyaW5nIj5BVUQ8L0F0dHJpYnV0ZVZhbHVlPgoJCQkJPC9NYXRjaD4KCQkJCTxNYXRjaCBNYXRjaElkPSJ1cm46b2FzaXM6bmFtZXM6dGM6eGFjbWw6MS4wOmZ1bmN0aW9uOnN0cmluZy1lcXVhbCIgPgoJCQkJCTxBdHRyaWJ1dGVEZXNpZ25hdG9yCgkJCQkJCQlBdHRyaWJ1dGVJZD0idXJuOm9hc2lzOm5hbWVzOnRjOnhhY21sOjEuMDpyZXNvdXJjZTpyZXNvdXJjZS1pZCIKCQkJCQkJCUNhdGVnb3J5PSJ1cm46b2FzaXM6bmFtZXM6dGM6eGFjbWw6My4wOmF0dHJpYnV0ZS1jYXRlZ29yeTpyZXNvdXJjZSIKCQkJCQkJCURhdGFUeXBlPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYSNzdHJpbmciCgkJCQkJCQlNdXN0QmVQcmVzZW50PSJ0cnVlIj4KCQkJCQk8L0F0dHJpYnV0ZURlc2lnbmF0b3I+CgkJCQk8QXR0cmlidXRlVmFsdWUKCQkJCQkJRGF0YVR5cGU9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hI3N0cmluZyI+UkVTPC9BdHRyaWJ1dGVWYWx1ZT4KCQkJCTwvTWF0Y2g+CgkJCTwvQWxsT2Y+CgkJPC9BbnlPZj4KCTwvVGFyZ2V0PgoJPFJ1bGUgRWZmZWN0PSJQZXJtaXQiIFJ1bGVJZD0icnVsZS1wZXJtaXQiPgoJCTxUYXJnZXQ+PC9UYXJnZXQ+CgkJPCEtLSBQcmUgY29uZGl0aW9uIC0tPgoJCTxDb25kaXRpb24gRGVjaXNpb25UaW1lPSJwcmUiPgoJCQk8QXBwbHkgRnVuY3Rpb25JZD0idXJuOm9hc2lzOm5hbWVzOnRjOnhhY21sOjEuMDpmdW5jdGlvbjphbmQiPgoJCQkJPEFwcGx5CgkJCQkJCUZ1bmN0aW9uSWQ9InVybjpvYXNpczpuYW1lczp0Yzp4YWNtbDoxLjA6ZnVuY3Rpb246c3RyaW5nLWVxdWFsIj4KCQkJCQk8QXBwbHkKCQkJCQkJCUZ1bmN0aW9uSWQ9InVybjpvYXNpczpuYW1lczp0Yzp4YWNtbDoxLjA6ZnVuY3Rpb246c3RyaW5nLW9uZS1hbmQtb25seSI+CgkJCQkJCTxBdHRyaWJ1dGVEZXNpZ25hdG9yCgkJCQkJCQkJQXR0cmlidXRlSWQ9InVybjpvYXNpczpuYW1lczp0Yzp4YWNtbDoxLjA6cmVzb3VyY2U6cmVzb3VyY2Utc2VydmVyIgoJCQkJCQkJCUNhdGVnb3J5PSJ1cm46b2FzaXM6bmFtZXM6dGM6eGFjbWw6My4wOmF0dHJpYnV0ZS1jYXRlZ29yeTpyZXNvdXJjZSIKCQkJCQkJCQlEYXRhVHlwZT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS9YTUxTY2hlbWEjc3RyaW5nIgoJCQkJCQkJCU11c3RCZVByZXNlbnQ9InRydWUiPgoJCQkJCQk8L0F0dHJpYnV0ZURlc2lnbmF0b3I+CgkJCQkJPC9BcHBseT4KCQkJCQk8QXR0cmlidXRlVmFsdWUKCQkJCQkJCURhdGFUeXBlPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYSNzdHJpbmciPkFVRDwvQXR0cmlidXRlVmFsdWU+CgkJCQk8L0FwcGx5PgoJCQkJPEFwcGx5CgkJCQkJCUZ1bmN0aW9uSWQ9InVybjpvYXNpczpuYW1lczp0Yzp4YWNtbDoxLjA6ZnVuY3Rpb246c3RyaW5nLWVxdWFsIj4KCQkJCQk8QXBwbHkKCQkJCQkJCUZ1bmN0aW9uSWQ9InVybjpvYXNpczpuYW1lczp0Yzp4YWNtbDoxLjA6ZnVuY3Rpb246c3RyaW5nLW9uZS1hbmQtb25seSI+CgkJCQkJCTxBdHRyaWJ1dGVEZXNpZ25hdG9yCgkJCQkJCQkJQXR0cmlidXRlSWQ9InVybjpvYXNpczpuYW1lczp0Yzp4YWNtbDoxLjA6YWN0aW9uOmFjdGlvbi1pZCIKCQkJCQkJCQlDYXRlZ29yeT0idXJuOm9hc2lzOm5hbWVzOnRjOnhhY21sOjMuMDphdHRyaWJ1dGUtY2F0ZWdvcnk6YWN0aW9uIgoJCQkJCQkJCURhdGFUeXBlPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYSNzdHJpbmciCgkJCQkJCQkJTXVzdEJlUHJlc2VudD0idHJ1ZSI+CgkJCQkJCTwvQXR0cmlidXRlRGVzaWduYXRvcj4KCQkJCQk8L0FwcGx5PgoJCQkJCTxBdHRyaWJ1dGVWYWx1ZQoJCQkJCQkJRGF0YVR5cGU9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hI3N0cmluZyI+T1A8L0F0dHJpYnV0ZVZhbHVlPgoJCQkJPC9BcHBseT4KCQkJCTxBcHBseQoJCQkJCQlGdW5jdGlvbklkPSJ1cm46b2FzaXM6bmFtZXM6dGM6eGFjbWw6MS4wOmZ1bmN0aW9uOnN0cmluZy1lcXVhbCI+CgkJCQkJPEFwcGx5CgkJCQkJCQlGdW5jdGlvbklkPSJ1cm46b2FzaXM6bmFtZXM6dGM6eGFjbWw6MS4wOmZ1bmN0aW9uOnN0cmluZy1vbmUtYW5kLW9ubHkiPgoJCQkJCQk8QXR0cmlidXRlRGVzaWduYXRvcgoJCQkJCQkJCUF0dHJpYnV0ZUlkPSJ1cm46b2FzaXM6bmFtZXM6dGM6eGFjbWw6MS4wOnJlc291cmNlOnJlc291cmNlLWlkIgoJCQkJCQkJCUNhdGVnb3J5PSJ1cm46b2FzaXM6bmFtZXM6dGM6eGFjbWw6My4wOmF0dHJpYnV0ZS1jYXRlZ29yeTpyZXNvdXJjZSIKCQkJCQkJCQlEYXRhVHlwZT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS9YTUxTY2hlbWEjc3RyaW5nIgoJCQkJCQkJCU11c3RCZVByZXNlbnQ9InRydWUiPgoJCQkJCQk8L0F0dHJpYnV0ZURlc2lnbmF0b3I+CgkJCQkJPC9BcHBseT4KCQkJCQk8QXR0cmlidXRlVmFsdWUKCQkJCQkJCURhdGFUeXBlPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYSNzdHJpbmciPlJFUzwvQXR0cmlidXRlVmFsdWU+CgkJCQk8L0FwcGx5PgoJCQk8L0FwcGx5PgoJCTwvQ29uZGl0aW9uPgoJCTwhLS0gT24gZ29pbmcgY29uZGl0aW9uIC0tPgoJCTxDb25kaXRpb24gRGVjaXNpb25UaW1lPSJvbmdvaW5nIj4KCQkJPEFwcGx5IEZ1bmN0aW9uSWQ9InVybjpvYXNpczpuYW1lczp0Yzp4YWNtbDoxLjA6ZnVuY3Rpb246YW5kIj4KCQkJCTxBcHBseQoJCQkJCQlGdW5jdGlvbklkPSJ1cm46b2FzaXM6bmFtZXM6dGM6eGFjbWw6MS4wOmZ1bmN0aW9uOnN0cmluZy1lcXVhbCIgPgoJCQkJCTxBcHBseQoJCQkJCQkJRnVuY3Rpb25JZD0idXJuOm9hc2lzOm5hbWVzOnRjOnhhY21sOjEuMDpmdW5jdGlvbjpzdHJpbmctb25lLWFuZC1vbmx5IiA+CgkJCQkJCTxBdHRyaWJ1dGVEZXNpZ25hdG9yCgkJCQkJCQkJQXR0cmlidXRlSWQ9InVybjpvYXNpczpuYW1lczp0Yzp4YWNtbDozLjA6ZW52aXJvbm1lbnQ6YXR0cmlidXRlLTEiCgkJCQkJCQkJQ2F0ZWdvcnk9InVybjpvYXNpczpuYW1lczp0Yzp4YWNtbDozLjA6YXR0cmlidXRlLWNhdGVnb3J5OmVudmlyb25tZW50IgoJCQkJCQkJCURhdGFUeXBlPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxL1hNTFNjaGVtYSNzdHJpbmciCgkJCQkJCQkJTXVzdEJlUHJlc2VudD0idHJ1ZSI+CgkJCQkJCTwvQXR0cmlidXRlRGVzaWduYXRvcj4KCQkJCQk8L0FwcGx5PgoJCQkJCTxBdHRyaWJ1dGVWYWx1ZQoJCQkJCQkJRGF0YVR5cGU9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hI3N0cmluZyIgPmF0dHJpYnV0ZS0xLXZhbHVlPC9BdHRyaWJ1dGVWYWx1ZT4KCQkJCTwvQXBwbHk+CgkJCTwvQXBwbHk+CgkJPC9Db25kaXRpb24+CgkJPCEtLSBQb3N0IGNvbmRpdGlvbiAtLT4KCQk8Q29uZGl0aW9uIERlY2lzaW9uVGltZT0icG9zdCI+CgkJCTxBcHBseSBGdW5jdGlvbklkPSJ1cm46b2FzaXM6bmFtZXM6dGM6eGFjbWw6MS4wOmZ1bmN0aW9uOmFuZCI+CgkJCQk8QXBwbHkKCQkJCQkJRnVuY3Rpb25JZD0idXJuOm9hc2lzOm5hbWVzOnRjOnhhY21sOjEuMDpmdW5jdGlvbjpzdHJpbmctZXF1YWwiPgoJCQkJCTxBcHBseQoJCQkJCQkJRnVuY3Rpb25JZD0idXJuOm9hc2lzOm5hbWVzOnRjOnhhY21sOjEuMDpmdW5jdGlvbjpzdHJpbmctb25lLWFuZC1vbmx5Ij4KCQkJCQkJPEF0dHJpYnV0ZURlc2lnbmF0b3IKCQkJCQkJCQlBdHRyaWJ1dGVJZD0idXJuOm9hc2lzOm5hbWVzOnRjOnhhY21sOjEuMDpyZXNvdXJjZTpyZXNvdXJjZS1pZCIKCQkJCQkJCQlDYXRlZ29yeT0idXJuOm9hc2lzOm5hbWVzOnRjOnhhY21sOjMuMDphdHRyaWJ1dGUtY2F0ZWdvcnk6cmVzb3VyY2UiCgkJCQkJCQkJRGF0YVR5cGU9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hI3N0cmluZyIKCQkJCQkJCQlNdXN0QmVQcmVzZW50PSJ0cnVlIj4KCQkJCQkJPC9BdHRyaWJ1dGVEZXNpZ25hdG9yPgoJCQkJCTwvQXBwbHk+CgkJCQkJPEF0dHJpYnV0ZVZhbHVlCgkJCQkJCQlEYXRhVHlwZT0iaHR0cDovL3d3dy53My5vcmcvMjAwMS9YTUxTY2hlbWEjc3RyaW5nIj5SRVM8L0F0dHJpYnV0ZVZhbHVlPgoJCQkJPC9BcHBseT4KCQkJPC9BcHBseT4KCQk8L0NvbmRpdGlvbj4KCTwvUnVsZT4KCTxSdWxlIEVmZmVjdD0iRGVueSIgUnVsZUlkPSJ1cm46b2FzaXM6bmFtZXM6dGM6eGFjbWw6My4wOmRlZmRlbnkiPgoJCTxEZXNjcmlwdGlvbj5EZWZhdWx0RGVueTwvRGVzY3JpcHRpb24+CgkJPFRhcmdldD48L1RhcmdldD4KCTwvUnVsZT4KPC9Qb2xpY3k+",
                            "policy_id": "policy_template2"
                        },
                        "id": "pap-web_socket",
                        "topic_name": "topic-name",
                        "topic_uuid": "topic-uuid-the-ucs-is-subscribed-to"
                    }
                }
            }
        }
    }
    print("\n--------- ADD POLICY -----------\n")
    print_and_send(ws_req)

## LIST POLICIES
def list_policies():
    ws_req = {
        "RequestPubMessage": {
            "value": {
                "timestamp": 1684256524618,
                "command": {
                    "command_type": "pap-command",
                    "value": {
                        "message": {
                            "purpose": "LIST_POLICIES",
                            "message_id": "random456-msg_id"
                        },
                        "id": "pap-web_socket",
                        "topic_name": "topic-name",
                        "topic_uuid": "topic-uuid-the-ucs-is-subscribed-to"
                    }
                }
            }
        }
    }
    print("\n-------- LIST POLICIES ---------\n")
    print_and_send(ws_req)

## GET POLICY
def get_policy():
    ws_req = {
        "RequestPubMessage": {
            "value": {
                "timestamp": 1684256524618,
                "command": {
                    "command_type": "pap-command",
                    "value": {
                        "message": {
                            "purpose": "GET_POLICY",
                            "message_id": "random456-msg_id",
                            "policy_id": "policy_template2"
                        },
                        "id": "pap-web_socket",
                        "topic_name": "topic-name",
                        "topic_uuid": "topic-uuid-the-ucs-is-subscribed-to"
                    }
                }
            }
        }
    }
    print("\n--------- GET POLICY -----------\n")
    print_and_send(ws_req)

## DELETE POLICY
def delete_policy():
    ws_req = {
        "RequestPubMessage": {
            "value": {
                "timestamp": 1684256524618,
                "command": {
                    "command_type": "pap-command",
                    "value": {
                        "message": {
                            "purpose": "DELETE_POLICY",
                            "message_id": "random456-msg_id",
                            "policy": None,
                            "policy_id": "policy_template2"
                        },
                        "id": "pap-web_socket",
                        "topic_name": "topic-name",
                        "topic_uuid": "topic-uuid-the-ucs-is-subscribed-to"
                    }
                }
            }
        }
    }
    print("\n-------- DELETE POLICY ---------\n")
    print_and_send(ws_req)

## UNRECOGNIZED COMMAND
def unrecognized_command():
    ws_req = {
        "RequestPostTopicUUID":{
            "value":{
                "log":{
                    "type":"info",
                    "priority":"low",
                    "category":"status",
                    "message":"ACE Group Manager"
                }
            },
            "topic_name":"SIFIS:Log",
            "topic_uuid":"Log",
            "deleted":"false"
        }
    }
    print("\n----- UNRECOGNIZED COMMAND -----\n")
    print_and_send(ws_req)

    print("\nMessage was sent. However, if the UCS does not recognize the command, "
          "discards it without responding.")
    print("\n--------------------------------\n")
    enter_command()


    ### GET POLICY (policy_XXX) (not present)
    #    ws_req = {
    #      "RequestPubMessage": {
    #        "value": {
    #          "timestamp": 1684256524618,
    #          "command": {
    #            "command_type": "pap-command",
    #            "value": {
    #              "message": {
    #                "purpose": "GET_POLICY",
    #                "message_id": "random456-msg_id",
    #                "policy_id": "policy_XXX"
    #              },
    #              "id": "pap-0",
    #              "topic_name": "topic-name",
    #              "topic_uuid": "topic-uuid-the-ucs-is-subscribed-to"
    #              }
    #            }
    #          }
    #        }
    #      }


if __name__ == "__main__":
    ws = websocket.WebSocketApp("ws://localhost:3000/ws",
                                on_open=on_open,
                                on_message=on_message,
                                on_error=on_error,
                                on_close=on_close)

    ws.run_forever()

