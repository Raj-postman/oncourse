export interface MoodleFields {
  baseUrl: string;
  username: string;
  password: string;
  serviceName: string;
  courseTag: string;
}

export interface MailchimpFields {
  apiKey: string;
  listId: string;
}

export interface SurveyMonkeyFields {
  apiKey: string;
  authToken: string;
  surveyName: string;
  courseTag: string;
  sendOnEnrolmentSuccess: boolean;
  sendOnEnrolmentCompletion: boolean;
}

export interface SurveyGizmoFields {
  user: string;
  password: string;
  surveyId: string;
  courseTag: string;
  sendOnEnrolmentSuccess: boolean;
  sendOnEnrolmentCompletion: boolean;
}

export interface XeroFields {
  apiKey: string;
}

export interface MYOBFields {
  baseUrl: string;
  user: string;
  password: string;
}

export interface CloudAssessFields {
  username: string;
  apiKey: string;
  orgId: string;
}

export interface CanvasFields {
  baseUrl: string;
  clientToken: string;
  clientSecret: string;
  accountId: string;
}

export interface MicropowerFields {
    identity: string;
    signature: string;
    clientId: string;
    productSku: string;
}

export interface CoassembleFields {
  baseUrl: string;
  apiKey: string;
  userId: string;
}