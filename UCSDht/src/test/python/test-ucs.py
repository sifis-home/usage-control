import json
import websocket
from pathlib import Path
import base64
import uuid
import datetime

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
    elif command == "q":
        exit("")
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
         "timestamp": int(datetime.datetime.now().timestamp()*1000),
         "command": {
           "command_type": "pep-command",
           "value": {
             "message": {
               "purpose": "REGISTER",
               "message_id": str(uuid.uuid1()),
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
    request = Path('../../../src/main/resources/example-request.xml').read_text()
    print("XACML request used:")
    print(request)
    b = base64.b64encode(bytes(request, 'utf-8')) # bytes
    request64 = b.decode('utf-8')

    ws_req = {
        "RequestPubMessage": {
            "value": {
                "timestamp": int(datetime.datetime.now().timestamp()*1000),
                "command": {
                    "command_type": "pep-command",
                    "value": {
                        "message": {
                            "purpose": "TRY",
                            "message_id": str(uuid.uuid1()),
                            "request": request64,
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
                "timestamp": int(datetime.datetime.now().timestamp()*1000),
                "command": {
                    "command_type": "pep-command",
                    "value": {
                        "message": {
                            "purpose": "START",
                            "message_id": str(uuid.uuid1()),
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
                "timestamp": int(datetime.datetime.now().timestamp()*1000),
                "command": {
                    "command_type": "pep-command",
                    "value": {
                        "message": {
                            "purpose": "END",
                            "message_id": str(uuid.uuid1()),
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
    policy = Path('../../../resources/example-policy.xml').read_text()
    print("XACML policy used:")
    print(policy)
    b = base64.b64encode(bytes(policy, 'utf-8')) # bytes
    policy64 = b.decode('utf-8')

    ws_req = {
        "RequestPubMessage": {
            "value": {
                "timestamp": int(datetime.datetime.now().timestamp()*1000),
                "command": {
                    "command_type": "pap-command",
                    "value": {
                        "message": {
                            "purpose": "ADD_POLICY",
                            "message_id": str(uuid.uuid1()),
                            "policy": policy64,
                            "policy_id": "example-policy"
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
                "timestamp": int(datetime.datetime.now().timestamp()*1000),
                "command": {
                    "command_type": "pap-command",
                    "value": {
                        "message": {
                            "purpose": "LIST_POLICIES",
                            "message_id": str(uuid.uuid1())
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
                "timestamp": int(datetime.datetime.now().timestamp()*1000),
                "command": {
                    "command_type": "pap-command",
                    "value": {
                        "message": {
                            "purpose": "GET_POLICY",
                            "message_id": str(uuid.uuid1()),
                            "policy_id": "example-policy"
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
                "timestamp": int(datetime.datetime.now().timestamp()*1000),
                "command": {
                    "command_type": "pap-command",
                    "value": {
                        "message": {
                            "purpose": "DELETE_POLICY",
                            "message_id": str(uuid.uuid1()),
                            "policy": None,
                            "policy_id": "example-policy"
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
    #          "timestamp": int(datetime.datetime.now().timestamp()*1000),
    #          "command": {
    #            "command_type": "pap-command",
    #            "value": {
    #              "message": {
    #                "purpose": "GET_POLICY",
    #                "message_id": str(uuid.uuid1()),
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
