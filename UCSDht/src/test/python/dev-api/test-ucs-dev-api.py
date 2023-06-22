import json
import websocket
import uuid
import datetime
import os
import base64

session_id = "None"

def load_xml_and_convert_to_base64(xml_file):
    if os.path.isfile(xml_file):
        with open(xml_file, 'r') as file:
            xml_string = file.read()
            b = base64.b64encode(bytes(xml_string, 'utf-8')) # bytes
            return b.decode('utf-8')
    else:
        print("[WARN] Unable to load " + xml_file + "\n")

turn_on = load_xml_and_convert_to_base64("./requests/turn_on.xml")
turn_off = load_xml_and_convert_to_base64("./requests/turn_off.xml")
get_status = load_xml_and_convert_to_base64("./requests/get_status.xml")
set_brightness_95 = load_xml_and_convert_to_base64("./requests/set_brightness_95.xml")
set_brightness_50 = load_xml_and_convert_to_base64("./requests/set_brightness_50.xml")
get_brightness = load_xml_and_convert_to_base64("./requests/get_brightness.xml")
policy = load_xml_and_convert_to_base64("./policies/execution-policy-app_name-lamp.xml")

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
    print("  LAMP EXAMPLE:")
    print("   -h : print a description about these new commands")
    print("   10 : add policy for the lamp")
    print("   11 : request permission to turn the lamp on")
    print("   12 : request permission to turn the lamp off")
    print("   13 : request permission to get the on/off state of the lamp")
    print("   14 : request permission to set the brightness to 95")
    print("   15 : request permission to set the brightness to 50")
    print("   16 : request permission to get the brightness value")
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
    elif command == "-h":
        print_description()
    elif command == "10":
        add_policy_lamp()
    elif command == "11":
        try_access_lamp(turn_on)
    elif command == "12":
        try_access_lamp(turn_off)
    elif command == "13":
        try_access_lamp(get_status)
    elif command == "14":
        try_access_lamp(set_brightness_95)
    elif command == "15":
        try_access_lamp(set_brightness_50)
    elif command == "16":
        try_access_lamp(get_brightness)
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
    req = load_xml_and_convert_to_base64("../../../main/resources/example-request.xml")
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
                            "request": req,
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
    pol = load_xml_and_convert_to_base64("../../../main/resources/example-policy.xml")
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
                            "policy": pol,
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


def print_description():

    print("The first eight commands show a complete 'usage control' example and\n"
          "uses the example-policy and the example-request XML files contained\n"
          "under the 'UCSDht/src/main/resources' directory.\n"
          "\n"
          "In this example, you can run the whole usage control flow by making\n"
          "a tryAccess, then a startAccess, and finally an endAccess request.\n"
          "Also, you can act as a PAP, so you can perform operations on the \n"
          "policies at the UCS.\n"
          "\n"
          "\n")
    print("Instead, in the 'lamp example', we have a policy stating that:\n"
          "- the app can turn the lamp on and off \n"
          "- the app cannot get the on/off state of the lamp\n"
          "- the app cannot set a brightness value greater than or equal to 80\n"
          "- the app can get the brightness value\n"
          "\n"
          "The requests and policy used are in the 'requests' and 'policies'\n"
          "folders\n"
          "\n"
          "Run the flow smoothly:\n"
          "1) start the UCS\n"
          "2) add the policy (command 10)\n"
          "3) register the PEP (command 1)\n"
          "4) make any request (commands 11..16)\n"
          "     the requests are try_access requests. The 'evaluation' field\n"
          "     in the response indicates if the permission is granted (and\n"
          "     therefore the dev-API should go on) or not (the dev-API \n"
          "     execution should be stopped.\n"
          "     For each try_access that evaluates to Permit, a session is \n"
          "     created at the UCS; so, a back-to-back start_access, and an\n"
          "     end_access (when the usage is terminated) should follow.\n")

    input("Press any key to continue...")
    enter_command()


def add_policy_lamp():
    global policy
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
                            "policy": policy,
                            "policy_id": "execution-policy-app_name-lamp"
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


def try_access_lamp(request):
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
                            "request": request,
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
