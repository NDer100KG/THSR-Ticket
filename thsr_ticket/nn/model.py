# -*- coding: utf-8 -*-
"""Model module

This module is the definition of CNN model.
"""
import torch
from torch import nn
import torch.nn.functional as F
import numpy as np

dic19 = {
    "2": 0,
    "3": 1,
    "4": 2,
    "5": 3,
    "7": 4,
    "9": 5,
    "a": 6,
    "c": 7,
    "f": 8,
    "h": 9,
    "k": 10,
    "m": 11,
    "n": 12,
    "p": 13,
    "q": 14,
    "r": 15,
    "t": 16,
    "y": 17,
    "z": 18,
}

class Flatten(nn.Module):
    """Flatten Module(layer).
    
       This model flatten input to (batch size, -1)
    """
    def forward(self, input):
        return input.view(input.size(0), -1)

class CNN(nn.Module):
    """CNN modle.
    
       Refernce https://github.com/JasonLiTW/simple-railway-captcha-solver 
    """
    def __init__(self):
        super(CNN, self).__init__()
        self.hidden1 = nn.Sequential(
            nn.Conv2d(1, 32, 3, padding=1, stride=2)
            ,nn.BatchNorm2d(32)
            ,nn.ReLU(inplace=True)
            ,nn.Conv2d(32, 32, 1, padding = 1)
            ,nn.BatchNorm2d(32)
            ,nn.ReLU(inplace=True)
        )

        self.hidden2 = nn.Sequential(
            nn.Conv2d(32, 64, 3, padding=1, stride=2)
            ,nn.BatchNorm2d(64)
            ,nn.ReLU(inplace=True)
            ,nn.Conv2d(64, 64, 1, padding = 1)
            ,nn.BatchNorm2d(64)
            ,nn.ReLU(inplace=True)
        )

        self.hidden3 = nn.Sequential(
            nn.Conv2d(64, 128, 3, padding=1, stride = 2)
            ,nn.BatchNorm2d(128)
            ,nn.ReLU(inplace=True)
            ,nn.Conv2d(128, 128, 1, padding = 1)
            ,nn.BatchNorm2d(128)
            ,nn.ReLU(inplace=True)
        )

        self.hidden4 = nn.Sequential(
            nn.Conv2d(128, 256, 3, padding = 1, stride= 2)
            ,nn.BatchNorm2d(256)
            ,nn.ReLU(inplace=True),
            nn.Conv2d(256, 256, 3, padding = 1, stride= 1)
            ,nn.BatchNorm2d(256)
            ,nn.ReLU(inplace=True)
        )
        
        self.hidden5 = nn.Sequential(
            nn.Conv2d(256, 256, 3, padding = 1, stride= 2)
            ,nn.BatchNorm2d(256)
            ,nn.ReLU(inplace=True)
        )

        self.flatten = Flatten()
        self.digit1 = nn.Linear(6400, 19)
        self.digit2 = nn.Linear(6400, 19)
        self.digit3 = nn.Linear(6400, 19)
        self.digit4 = nn.Linear(6400, 19)

    def forward(self, inputs):
        x = self.hidden1(inputs)
        x = self.hidden2(x)
        x = self.hidden3(x)
        x = self.hidden4(x)
        x = self.hidden5(x) # (2, 256, 5, 5)
        x = self.flatten(x)
        digit1 = torch.nn.functional.softmax(self.digit1(x), dim=1)
        digit2 = torch.nn.functional.softmax(self.digit2(x), dim=1)
        digit3 = torch.nn.functional.softmax(self.digit3(x), dim=1)
        digit4 = torch.nn.functional.softmax(self.digit4(x), dim=1)
       
        return digit1, digit2, digit3, digit4

    def save(self, path):
        """Save parameters of model.

        Args:
            path(str): parameters file path.

        """
        torch.save(self.state_dict(), path)
        # torch.save(self, path)
    
    def load(self, path):
        """Load parameters of model.

        Args:
            path(str): parameters file path.

        """
        self.load_state_dict(torch.load(path, map_location=lambda storage, loc: storage))
        # torch.load(path)

    @staticmethod
    def decode(code):
        """Decode the CNN output.

        Args:
            scores (tensor): CNN output.

        Returns:
            list(int): list include each digit index.
        """
        tmp = np.array(tuple(map(lambda score: score.cpu().numpy(), code)))
        tmp = np.swapaxes(tmp, 0, 1)

        decode_dict = {value: key for key, value in dic19.items()}
        result = []
        for c in np.argmax(tmp, axis=2)[0]:
            result.append(decode_dict[int(c)])

        return result

if __name__ == "__main__":
    from torchsummary import summary
    cnn = CNN()

    summary(cnn.cuda(), (1, 128, 128))